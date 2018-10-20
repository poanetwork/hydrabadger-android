//! A hydrabadger consensus node.
//!

#![allow(unused_imports, dead_code, unused_variables, unused_mut, unused_assignments,
    unreachable_code)]

use std::{
    time::{Duration, Instant},
    sync::{
        atomic::{AtomicUsize, Ordering},
        Arc,
    },
    collections::HashSet,
    net::{SocketAddr, ToSocketAddrs},
};
use futures::{
    sync::mpsc,
    future::{self, Either},
};
use tokio::{
    self,
    net::{TcpListener, TcpStream},
    timer::Interval,
    prelude::*,
};
use rand::{self, Rand};
use parking_lot::{RwLock, Mutex, RwLockReadGuard, RwLockWriteGuard};
use hbbft::{
    crypto::{PublicKey, SecretKey},
    queueing_honey_badger::{Input as QhbInput},
};
use peer::{PeerHandler, Peers};
use ::{InternalMessage, WireMessage, WireMessageKind, WireMessages,
    OutAddr, InAddr,  Uid, InternalTx, Transaction};
use super::{Error, State, StateDsct, Handler};


// The HoneyBadger batch size.
const DEFAULT_BATCH_SIZE: usize = 200;
// The number of random transactions to generate per interval.
const DEFAULT_TXN_GEN_COUNT: usize = 5;
// The interval between randomly generated transactions.
const DEFAULT_TXN_GEN_INTERVAL: u64 = 5000;
// The number of bytes per randomly generated transaction.
const DEFAULT_TXN_GEN_BYTES: usize = 2;
// The minimum number of peers needed to spawn a HB instance.
const DEFAULT_KEYGEN_PEER_COUNT: usize = 2;
// Causes the primary hydrabadger thread to sleep after every batch. Used for
// debugging.
const DEFAULT_OUTPUT_EXTRA_DELAY_MS: u64 = 10;


#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Config {
    pub batch_size: usize,
    pub txn_gen_count: usize,
    pub txn_gen_interval: u64,
    // TODO: Make this a range:
    pub txn_gen_bytes: usize,
    pub keygen_peer_count: usize,
    pub output_extra_delay_ms: u64,
}

impl Config {
    pub fn with_defaults() -> Config {
        Config {
            batch_size: DEFAULT_BATCH_SIZE,
            txn_gen_count: DEFAULT_TXN_GEN_COUNT,
            txn_gen_interval: DEFAULT_TXN_GEN_INTERVAL,
            txn_gen_bytes: DEFAULT_TXN_GEN_BYTES,
            keygen_peer_count: DEFAULT_KEYGEN_PEER_COUNT,
            output_extra_delay_ms: DEFAULT_OUTPUT_EXTRA_DELAY_MS,
        }
    }
}

impl Default for Config {
    fn default() -> Config {
        Config::with_defaults()
    }
}


/// The `Arc` wrapped portion of `Hydrabadger`.
///
/// Shared all over the place.
struct Inner {
    /// Node uid:
    uid: Uid,
    /// Incoming connection socket.
    addr: InAddr,

    addr_out: InAddr,
    addr_out_: SocketAddr,

    /// This node's secret key.
    secret_key: SecretKey,

    peers: RwLock<Peers>,

    /// The current state containing HB when connected.
    state: RwLock<State>,

    // TODO: Move this into a new state struct.
    state_dsct: AtomicUsize,

    // TODO: Use a bounded tx/rx (find a sensible upper bound):
    peer_internal_tx: InternalTx,

    config: Config,
}


type CallbackBatch = fn(num: i32, its_me: bool, id: String, trans: String);

/// A `HoneyBadger` network node.
#[derive(Clone)]
pub struct Hydrabadger {
    inner: Arc<Inner>,
    handler: Arc<Mutex<Option<Handler>>>,
     // callback Batch
    callbackbatch: CallbackBatch,
    num: i32,
}

impl Hydrabadger {
    /// Returns a new Hydrabadger node.
    pub fn new(addr: SocketAddr, addr_out: SocketAddr, cfg: Config, callbackbatch: CallbackBatch, num: i32) -> Self {
        use std::env;
        use env_logger;
        use chrono::Local;

        let uid = Uid::new();
        let secret_key = SecretKey::rand(&mut rand::thread_rng());

        let (peer_internal_tx, peer_internal_rx) = mpsc::unbounded();

        warn!("");
        warn!(" {} - Local Hydrabadger Node: ", num);
        warn!(" {} -     UID:             {}", num, uid);
        warn!(" {} -     Socket Address:  {}", num, addr);
        warn!(" {} -     Socket Address:  {}", num, addr_out);
        warn!(" {} -     Public Key:      {:?}", num, secret_key.public_key());

        warn!("");
        warn!(" {} - ****** This is an alpha build. Do not use in production! ******", num);
        warn!("");

        let inner = Arc::new(Inner {
            uid,
            addr: InAddr(addr),
            addr_out: InAddr(addr_out),
            addr_out_: addr_out,
            secret_key,
            peers: RwLock::new(Peers::new()),
            state: RwLock::new(State::disconnected()),
            state_dsct: AtomicUsize::new(0),
            peer_internal_tx,
            config: cfg,
        });

        let hdb = Hydrabadger {
            inner,
            handler: Arc::new(Mutex::new(None)),
            callbackbatch,
            num,
        };


        *hdb.handler.lock() = Some(Handler::new(hdb.clone(), peer_internal_rx, callbackbatch, num));

        hdb
    }

    /// Returns the pre-created handler.
    pub fn handler(&self) -> Option<Handler> {
        self.handler.lock().take()
    }

    /// Returns a reference to the inner state.
    pub(crate) fn state(&self) -> RwLockReadGuard<State> {
        let state = self.inner.state.read();
        state
    }

    /// Returns a mutable reference to the inner state.
    pub(crate) fn state_mut(&self) -> RwLockWriteGuard<State> {
        let state = self.inner.state.write();
        state
    }

    /// Returns a recent state discriminant.
    ///
    /// The returned value may not be up to date and is to be considered
    /// immediately stale.
    pub fn state_info_stale(&self) -> (StateDsct, usize, usize) {
        let sd = self.inner.state_dsct.load(Ordering::Relaxed).into();
        (sd, 0, 0)
    }

    /// Sets the publicly visible state discriminant and returns the previous value.
    pub(super) fn set_state_discriminant(&self, dsct: StateDsct) -> StateDsct {
        let sd = StateDsct::from(self.inner.state_dsct.swap(dsct.into(), Ordering::Release));
        warn!(" {} - State has been set from '{}' to '{}'.", self.num, sd, dsct);
        sd
    }

    /// Returns a reference to the peers list.
    pub(crate) fn peers(&self) -> RwLockReadGuard<Peers> {
        self.inner.peers.read()
    }

    /// Returns a mutable reference to the peers list.
    pub(crate) fn peers_mut(&self) -> RwLockWriteGuard<Peers> {
        self.inner.peers.write()
    }

    /// Returns a mutable reference to the peers list.
    pub(crate) fn config(&self) -> &Config {
        &self.inner.config
    }

    /// Sends a message on the internal tx.
    pub(crate) fn send_internal(&self, msg: InternalMessage) {
        if let Err(err) = self.inner.peer_internal_tx.unbounded_send(msg) {
            error!("Unable to send on internal tx. Internal rx has dropped: {}", err);
            ::std::process::exit(-1)
        }
    }

    /// Returns a future that handles incoming connections on `socket`.
    fn handle_incoming(self, socket: TcpStream)
            -> impl Future<Item = (), Error = ()> {
        warn!("!! {} - Incoming connection from '{}'", self.num, socket.peer_addr().unwrap());
        let wire_msgs = WireMessages::new(socket);

        wire_msgs.into_future()
            .map_err(|(e, _)| e)
            .and_then(move |(msg_opt, w_messages)| {
                // let _hdb = self.clone();

                match msg_opt {
                    Some(msg) => match msg.into_kind() {
                        // The only correct entry point:
                        WireMessageKind::HelloRequestChangeAdd(peer_uid, peer_in_addr, peer_pk) => {
                            warn!("!! {} - Peer connected with sending \
                                `WireMessageKind::HelloRequestChangeAdd` completed.", self.num);

                            // Also adds a `Peer` to `self.peers`.
                            let peer_h = PeerHandler::new(Some((peer_uid, peer_in_addr, peer_pk)),
                                self.clone(), w_messages);

                            // Relay incoming `HelloRequestChangeAdd` message internally.
                            peer_h.hdb().send_internal(
                                InternalMessage::new_incoming_connection(peer_uid,
                                    *peer_h.out_addr(), peer_in_addr, peer_pk, true)
                            );

                            warn!("!!! {} - handle_incoming!!!!!!!!!!!!!!!!!!!  peer_in_addr {} out_addr {}", self.num, peer_in_addr, *peer_h.out_addr());

                            Either::B(peer_h)
                        },
                        _ => {
                            // TODO: Return this as a future-error (handled below):
                            error!("!! {} - Peer connected without sending \
                                `WireMessageKind::HelloRequestChangeAdd`.", self.num);
                            Either::A(future::ok(()))
                        },
                    },
                    None => { 
                        warn!("!! {} - Incoming connection from  None msg_kind", self.num);
                        // The remote client closed the connection without sending
                        // a welcome_request_change_add message.
                        Either::A(future::ok(()))
                    },
                }
            })
            .map_err(|err| error!("!Connection error = {:?}", err))
    }

    /// Returns a future that connects to new peer.
    pub(super) fn connect_outgoing(self, remote_addr: SocketAddr, local_pk: PublicKey,
            pub_info: Option<(Uid, InAddr, PublicKey)>, is_optimistic: bool)
            -> impl Future<Item = (), Error = ()> {
        let uid = self.inner.uid.clone();
        let in_addr = self.inner.addr;
        let in_addr_out = self.inner.addr_out;
        

        // if remote_addr != in_addr_out {
            warn!("!! {} - Initiating outgoing connection to remote_addr: {}  in_addr_out: {}", self.num, remote_addr, in_addr_out);

            TcpStream::connect(&remote_addr)
                .map_err(Error::from)
                .and_then(move |socket| {
                    // Wrap the socket with the frame delimiter and codec:
                    let mut wire_msgs = WireMessages::new(socket);
    
                    warn!("!!! {} - send info Initiating outgoing connection!!!!!!!!!!!!!!!!!!!  {}", self.num, in_addr_out);
    
                    let wire_hello_result = wire_msgs.send_msg(
                        WireMessage::hello_request_change_add(uid, in_addr_out, local_pk));
                    match wire_hello_result {
                        Ok(_) => {
                            warn!("!! {} - connect_outgoing with sending \
                                    `WireMessageKind::HelloRequestChangeAdd` completed.", self.num);
    
                            let peer = PeerHandler::new(pub_info, self.clone(), wire_msgs);
    
                            warn!("!!! {} - result Initiating outgoing connection!!!!!!!!!!!!!!!!!!! pub_info {:?} out_addr {}", self.num, pub_info, *peer.out_addr());
    
                            self.send_internal(InternalMessage::new_outgoing_connection(*peer.out_addr()));
    
                            Either::A(peer)
                        },
                        Err(err) => Either::B(future::err(err)),
                    }
                })
                .map_err(move |err| {
                    if is_optimistic {
                        warn!("!! - Unable to connect to: {} \n{:?}" , remote_addr, err);
                    } else {
                        warn!("!! - Error connecting to: {} \n{:?}", remote_addr, err);
                    }
                })
        // }
    }

    /// Returns a future that generates random transactions and logs status
    /// messages.
    fn generate_txns_status(self) -> impl Future<Item = (), Error = ()> {
        Interval::new(Instant::now(), Duration::from_millis(self.inner.config.txn_gen_interval))
            .for_each(move |_| {
                let hdb = self.clone();
                let peers = hdb.peers();

                // Log state:
                let (dsct, p_ttl, p_est) = hdb.state_info_stale();
                let peer_count = peers.count_total();
                warn!(" {} - State: {:?}({})", self.num, dsct, peer_count);

                // Log peer list:
                let peer_list = peers.peers().map(|p| {
                    p.in_addr().map(|ia| ia.0.to_string())
                        .unwrap_or(format!("No in address"))
                }).collect::<Vec<_>>();
                warn!("  {} -    Peers: {:?}", self.num, peer_list);

                // Log (trace) full peerhandler details:
                warn!(" {} - PeerHandler list:", self.num);
                for (peer_addr, _peer) in peers.iter() {
                    warn!(" {} -      peer_addr: {}", self.num, peer_addr); }

                drop(peers);

                match dsct {
                    StateDsct::Validator => {
                        if self.num == 0 {
                             let txns = Transaction::get_tr1();
                            if !txns.is_empty() {
                                warn!("!! {} - send_internal {}", self.num, *hdb.inner.addr);
                                hdb.send_internal (
                                    InternalMessage::hb_input(hdb.inner.uid, OutAddr(*hdb.inner.addr), QhbInput::User(txns))
                                );
                                warn!("!! {} - send_internal__ ", self.num);
                            }
                        }
                        if self.num == 1 {
                            let txns = Transaction::get_tr2();
                            if !txns.is_empty() {
                                warn!("!! {} - send_internal {}", self.num, *hdb.inner.addr);
                                hdb.send_internal (
                                    InternalMessage::hb_input(hdb.inner.uid, OutAddr(*hdb.inner.addr), QhbInput::User(txns))
                                );
                                warn!("!! {} - send_internal__", self.num);
                            }
                        }
                        if self.num == 2 {
                            let txns = Transaction::get_tr3();
                            if !txns.is_empty() {
                                warn!("!! {} - send_internal {}", self.num, *hdb.inner.addr);
                                hdb.send_internal (
                                    InternalMessage::hb_input(hdb.inner.uid, OutAddr(*hdb.inner.addr), QhbInput::User(txns))
                                );
                                warn!("!! {} - send_internal__", self.num);
                            }
                        }
                    },
                    _ => {},
                }

                Ok(())
            })
            .map_err(|err| error!("List connection inverval error: {:?}", err))
    }


    /// Binds to a host address and returns a future which starts the node.
    pub fn node(self, remotes: Option<HashSet<SocketAddr>>, reactor_remote: Option<()>)
            -> impl Future<Item = (), Error = ()> {
        let socket = TcpListener::bind(&self.inner.addr).unwrap();
        // let mut socket = TcpStream::connect(&self.inner.addr).unwrap();
        warn!(" {} - Listening on: {}", self.num, self.inner.addr);

        let remotes = remotes.unwrap_or(HashSet::new());

        let hdb = self.clone();
        let listen = socket.incoming()
            .map_err(|err| error!("Error accepting socket: {:?}", err))
            .for_each(move |socket| {
                tokio::spawn(hdb.clone().handle_incoming(socket));
                Ok(())
            });

        let hdb = self.clone();
        let local_pk = hdb.inner.secret_key.public_key();
        let connect = future::lazy(move || {
            for &remote_addr in remotes.iter() {
                if remote_addr != hdb.clone().inner.addr_out_ {
                    warn!(" {} - connect_outgoing remote_addr: {}", hdb.clone().num, hdb.clone().inner.addr_out);
                    tokio::spawn(hdb.clone().connect_outgoing(remote_addr, local_pk, None, true));
                } 
            }
            Ok(())
        });

        let generate_txns_status = self.clone().generate_txns_status();

        let hdb_handler = self.handler()
            .map_err(|err| error!("Handler internal error: {:?}", err));

        listen.join4(connect, generate_txns_status, hdb_handler).map(|(_, _, _, _)| ())
    }

    /// Starts a node.
    pub fn run_node(self, remotes: Option<HashSet<SocketAddr>>) {
        tokio::run(self.node(remotes, None));
    }

    pub fn addr(&self) -> &InAddr {
        &self.inner.addr
    }

    pub fn uid(&self) -> &Uid {
        &self.inner.uid
    }

    pub(super) fn secret_key(&self) -> &SecretKey {
        &self.inner.secret_key
    }
}
