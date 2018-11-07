
#![allow(unused_imports, dead_code, unused_variables, unused_mut, unused_assignments,
    unreachable_code)]

use super::{Error, Handler, State, StateDsct};
use futures::{
    future::{self, Either},
    sync::mpsc,
};
use hbbft::{
    crypto::{PublicKey, SecretKey},
    dynamic_honey_badger::Input as DhbInput,
};
use parking_lot::{Mutex, RwLock, RwLockReadGuard, RwLockWriteGuard};
use peer::{PeerHandler, Peers};
use rand::{self, Rand};
use serde::{Deserialize, Serialize};
use std::{
    collections::HashSet,
    net::{SocketAddr, ToSocketAddrs},
    sync::{
        atomic::{AtomicUsize, Ordering},
        Arc, Weak,
    },
    time::{Duration, Instant},
};
use tokio::{
    self,
    net::{TcpListener, TcpStream},
    prelude::*,
    timer::{Interval, Delay},
};
use {
    Contribution, InAddr, InternalMessage, InternalTx, OutAddr, Uid, WireMessage, WireMessageKind,
    WireMessages, BatchRx, EpochTx, EpochRx, Transaction,
};

// The number of random transactions to generate per interval.
const DEFAULT_TXN_GEN_COUNT: usize = 1;
// The interval between randomly generated transactions.
const DEFAULT_TXN_GEN_INTERVAL: u64 = 5000;
// The number of bytes per randomly generated transaction.
const DEFAULT_TXN_GEN_BYTES: usize = 2;
// The minimum number of peers needed to spawn a HB instance.
const DEFAULT_KEYGEN_PEER_COUNT: usize = 2;
// Causes the primary hydrabadger thread to sleep after every batch. Used for
// debugging.
const DEFAULT_OUTPUT_EXTRA_DELAY_MS: u64 = 0;

/// Hydrabadger configuration options.
//
// TODO: Convert to builder.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Config {
    pub txn_gen_count: usize,
    pub txn_gen_interval: u64,
    // TODO: Make this a range:
    pub txn_gen_bytes: usize,
    pub keygen_peer_count: usize,
    pub output_extra_delay_ms: u64,
    pub start_epoch: u64,
}

impl Config {
    pub fn with_defaults() -> Config {
        Config {
            txn_gen_count: DEFAULT_TXN_GEN_COUNT,
            txn_gen_interval: DEFAULT_TXN_GEN_INTERVAL,
            txn_gen_bytes: DEFAULT_TXN_GEN_BYTES,
            keygen_peer_count: DEFAULT_KEYGEN_PEER_COUNT,
            output_extra_delay_ms: DEFAULT_OUTPUT_EXTRA_DELAY_MS,
            start_epoch: 0,
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
struct Inner<T: Contribution> {
    /// Node uid:
    uid: Uid,
    /// Incoming connection socket.
    addr: InAddr,

    // android fix
    addr_out: SocketAddr,

    /// This node's secret key.
    secret_key: SecretKey,

    peers: RwLock<Peers<T>>,

    /// The current state containing HB when connected.
    state: RwLock<State<T>>,

    // TODO: Move this into a new state struct.
    state_dsct: AtomicUsize,

    // TODO: Use a bounded tx/rx (find a sensible upper bound):
    peer_internal_tx: InternalTx<T>,

    /// The earliest epoch from which we have not yet received output.
    //
    // Used as an initial value when a new epoch listener is registered.
    current_epoch: Mutex<u64>,

    // TODO: Create a separate type which uses a hashmap internally and allows
    // for Tx removal. Altenratively just `Option` wrap Txs.
    epoch_listeners: RwLock<Vec<EpochTx>>,

    config: Config,
}

// android fix
type CallbackBatch = fn(num: i32, its_me: bool, id: String, trans: String);


/// A `HoneyBadger` network node.
#[derive(Clone)]
pub struct Hydrabadger<T: Contribution> {
    inner: Arc<Inner<T>>,
    handler: Arc<Mutex<Option<Handler<T>>>>,
    batch_rx: Arc<Mutex<Option<BatchRx<T>>>>,

    // android fix
    callbackbatch: CallbackBatch,
    num: i32,
}

impl<T: Contribution> Hydrabadger<T>  {
    /// Returns a new Hydrabadger node.
    pub fn new(addr: SocketAddr, addr_out: SocketAddr, cfg: Config, callbackbatch: CallbackBatch, num: i32) -> Self {
        use chrono::Local;
        use env_logger;
        use std::env;

        let uid = Uid::new();
        let secret_key = SecretKey::rand(&mut rand::thread_rng());

        let (peer_internal_tx, peer_internal_rx) = mpsc::unbounded();
        let (batch_tx, batch_rx) = mpsc::unbounded();

        info!("");
        info!("Local Hydrabadger Node: ");
        info!("    UID:             {}", uid);
        info!("    Socket Address:  {}", addr);
        info!("    Public Key:      {:?}", secret_key.public_key());

        warn!("");
        warn!("****** This is an alpha build. Do not use in production! ******");
        warn!("");

        let current_epoch = cfg.start_epoch;

        let inner = Arc::new(Inner {
            uid,
            addr: InAddr(addr),
            // android fix
            addr_out: addr_out,

            secret_key,
            peers: RwLock::new(Peers::new()),
            state: RwLock::new(State::disconnected()),
            state_dsct: AtomicUsize::new(0),
            peer_internal_tx,
            config: cfg,
            current_epoch: Mutex::new(current_epoch),
            epoch_listeners: RwLock::new(Vec::new()),
        });

        let hdb = Hydrabadger {
            inner,
            handler: Arc::new(Mutex::new(None)),
            batch_rx: Arc::new(Mutex::new(Some(batch_rx))),

            // android fix
            callbackbatch,
            num,
        };

        // android fix
        *hdb.handler.lock() = Some(Handler::new(hdb.clone(), peer_internal_rx, batch_tx, callbackbatch, num, InAddr(addr), InAddr(addr_out)));

        hdb
    }


    /// Returns a new Hydrabadger node.
    // pub fn with_defaults(addr: SocketAddr) -> Self {
    //     Hydrabadger::new(addr, Config::default())
    // }

    /// Returns the pre-created handler.
    pub fn handler(&self) -> Option<Handler<T>> {
        self.handler.lock().take()
    }

    /// Returns the batch output receiver.
    pub fn batch_rx(&self) -> Option<BatchRx<T>> {
        self.batch_rx.lock().take()
    }

    /// Returns a reference to the inner state.
    pub fn state(&self) -> RwLockReadGuard<State<T>> {
        self.inner.state.read()
    }

    /// Returns a mutable reference to the inner state.
    pub(crate) fn state_mut(&self) -> RwLockWriteGuard<State<T>> {
        let state = self.inner.state.write();
        state
    }

    /// Sets the publicly visible state discriminant and returns the previous value.
    pub(super) fn set_state_discriminant(&self, dsct: StateDsct) -> StateDsct {
        let sd = StateDsct::from(self.inner.state_dsct.swap(dsct.into(), Ordering::Release));
        info!("State has been set from '{}' to '{}'.", sd, dsct);
        sd
    }

    /// Returns a recent state discriminant.
    ///
    /// The returned value may not be up to date and is to be considered
    /// immediately stale.
    pub fn state_info_stale(&self) -> (StateDsct, usize, usize) {
        let sd = self.inner.state_dsct.load(Ordering::Relaxed).into();
        (sd, 0, 0)
    }

    pub fn is_validator(&self) -> bool {
        StateDsct::from(self.inner.state_dsct.load(Ordering::Relaxed)) == StateDsct::Validator
    }

    /// Returns a reference to the peers list.
    pub fn peers(&self) -> RwLockReadGuard<Peers<T>> {
        self.inner.peers.read()
    }

    /// Returns a mutable reference to the peers list.
    pub(crate) fn peers_mut(&self) -> RwLockWriteGuard<Peers<T>> {
        self.inner.peers.write()
    }

    /// Sets the current epoch and returns the previous epoch.
    ///
    /// The returned value should (always?) be equal to `epoch - 1`.
    //
    // TODO: Convert to a simple incrementer?
    pub(crate) fn set_current_epoch(&self, epoch: u64) -> u64 {
        let mut ce = self.inner.current_epoch.lock();
        let prev_epoch = *ce;
        *ce = epoch;
        prev_epoch
    }

    /// Returns the epoch of the next batch to be output.
    pub fn current_epoch(&self) -> u64 {
        *self.inner.current_epoch.lock()
    }

    /// Returns a stream of epoch numbers (e) indicating that a batch has been
    /// output for an epoch (e - 1) and that a new epoch has begun.
    ///
    /// The current epoch will be sent upon registration. If a listener is
    /// registered before any batches have been output by this instance of
    /// Hydrabadger, the start epoch will be output.
    pub fn register_epoch_listener(&self) -> EpochRx {
        let (tx, rx) = mpsc::unbounded();
        if self.is_validator() {
            tx.unbounded_send(self.current_epoch())
                .expect("Unknown error: receiver can not have dropped");
        }
        self.inner.epoch_listeners.write().push(tx);
        rx
    }

    /// Returns a reference to the epoch listeners list.
    pub(crate) fn epoch_listeners(&self) -> RwLockReadGuard<Vec<EpochTx>> {
        self.inner.epoch_listeners.read()
    }

    /// Returns a reference to the config.
    pub(crate) fn config(&self) -> &Config {
        &self.inner.config
    }

    /// Sends a message on the internal tx.
    pub(crate) fn send_internal(&self, msg: InternalMessage<T>) {
        if let Err(err) = self.inner.peer_internal_tx.unbounded_send(msg) {
            error!(
                "Unable to send on internal tx. Internal rx has dropped: {}",
                err
            );
            ::std::process::exit(-1)
        }
    }

    /// Handles a incoming batch of user transactions.
    pub fn propose_user_contribution(&self, txn: T) -> Result<(), Error> {
        if self.is_validator() {
            self.send_internal(InternalMessage::hb_input(
                self.inner.uid,
                OutAddr(*self.inner.addr),
                DhbInput::User(txn),
            ));
            Ok(())
        } else {
            Err(Error::ProposeUserContributionNotValidator)
        }
    }

    /// Returns a future that handles incoming connections on `socket`.
    fn handle_incoming(self, socket: TcpStream) -> impl Future<Item = (), Error = ()> {
        warn!("!! {} - Incoming connection from '{}'", self.num, socket.peer_addr().unwrap());

        let wire_msgs = WireMessages::new(socket);
        wire_msgs
            .into_future()
            .map_err(|(e, _)| e)
            .and_then(move |(msg_opt, w_messages)| {

                match msg_opt {
                    Some(msg) => match msg.into_kind() {
                        // The only correct entry point:
                        WireMessageKind::HelloRequestChangeAdd(peer_uid, peer_in_addr, peer_pk) => {
                            warn!("!! {} - Peer connected with sending \
                                `WireMessageKind::HelloRequestChangeAdd` completed. peer_in_addr - {}", self.num, peer_in_addr);

                            // Also adds a `Peer` to `self.peers`.
                            let peer_h = PeerHandler::new(
                                Some((peer_uid, peer_in_addr, peer_pk)),
                                self.clone(),
                                w_messages,
                            );

                            // Relay incoming `HelloRequestChangeAdd` message internally.
                            peer_h
                                .hdb()
                                .send_internal(InternalMessage::new_incoming_connection(
                                    peer_uid,
                                    *peer_h.out_addr(),
                                    peer_in_addr,
                                    peer_pk,
                                    true,
                                ));

                            warn!("!!! {} - handle_incoming!!!!!!!!!!!!!!!!!!!  peer_in_addr {} out_addr {}", self.num, peer_in_addr, *peer_h.out_addr());
    
                            Either::B(peer_h)
                        }
                        _ => {
                            warn!("!! {} - Incoming connection from  None msg_kind", self.num);
                            // TODO: Return this as a future-error (handled below):
                            error!(
                                "Peer connected without sending \
                                 `WireMessageKind::HelloRequestChangeAdd`."
                            );
                            Either::A(future::ok(()))
                        }
                    },
                    None => {
                        // The remote client closed the connection without sending
                        // a welcome_request_change_add message.
                        Either::A(future::ok(()))
                    }
                }
            })
            .map_err(|err| error!("Connection error = {:?}", err))
    }


    /// Returns a future that connects to new peer.
    pub(super) fn connect_outgoing(
        self,
        remote_addr: SocketAddr,
        local_pk: PublicKey,
        pub_info: Option<(Uid, InAddr, PublicKey)>,
        is_optimistic: bool,
    ) -> impl Future<Item = (), Error = ()> {
        let uid = self.inner.uid.clone();
        let in_addr = self.inner.addr;
        let in_addr_out = self.inner.addr_out;

        warn!("!! {} - Initiating outgoing connection to remote_addr: {}  in_addr_out: {}", self.num, remote_addr, in_addr_out);

        TcpStream::connect(&remote_addr)
            .map_err(Error::from)
            .and_then(move |socket| {
                // Wrap the socket with the frame delimiter and codec:
                let mut wire_msgs = WireMessages::new(socket);

                warn!("!!! {} - send info Initiating outgoing connection!!!!!!!!!!!!!!!!!!!  {}", self.num, in_addr_out);

                // android fix
                let wire_hello_result = wire_msgs.send_msg(WireMessage::hello_request_change_add(
                    uid, InAddr(in_addr_out), local_pk,
                ));
                match wire_hello_result {
                    Ok(_) => {
                            warn!("!! {} - connect_outgoing with sending \
                                    `WireMessageKind::HelloRequestChangeAdd` completed.", self.num);

                        let peer = PeerHandler::new(pub_info, self.clone(), wire_msgs);

                        warn!("!!! {} - result Initiating outgoing connection!!!!!!!!!!!!!!!!!!! pub_info {:?} out_addr {}", self.num, pub_info, *peer.out_addr());

                        self.send_internal(InternalMessage::new_outgoing_connection(
                            *peer.out_addr(),
                        ));

                        Either::A(peer)
                    }
                    Err(err) => Either::B(future::err(err)),
                }
            })
            .map_err(move |err| {
                if is_optimistic {
                    warn!("Unable to connect to: {}", remote_addr);
                } else {
                    error!("Error connecting to: {} \n{:?}", remote_addr, err);
                }
            })
    }

    fn generate_contributions(self, 
        gen_txns: Option<fn() -> T>,
        is_empty: fn() -> bool,
    )
        -> impl Future<Item = (), Error = ()>
    {
        if let Some(gen_txns) = gen_txns {
            let epoch_stream = self.register_epoch_listener();
            let gen_delay = self.inner.config.txn_gen_interval;
            let gen_cntrb = epoch_stream
                .and_then(move |epoch_no| {
                    Delay::new(Instant::now() + Duration::from_millis(gen_delay))
                        .map_err(|err| panic!("Timer error: {:?}", err))
                        .and_then(move |_| Ok(epoch_no))
                })
                .for_each(move |_epoch_no| {
                    let hdb = self.clone();

                    warn!("!! generate_contributions  {}", self.num);

                    match hdb.state_info_stale().0 {
                        StateDsct::Validator => {
                            warn!("!! hdb.state_info_stale().0 StateDsct::Validator {}", self.num);

                            // info!(
                            //     "Generating and inputting {} random transactions...",
                            //     self.inner.config.txn_gen_count
                            // );
                            // Send some random transactions to our internal HB instance.
                            // let txns = gen_txns();

                            // hdb.send_internal(InternalMessage::hb_input(
                            //     hdb.inner.uid,
                            //     OutAddr(*hdb.inner.addr),
                            //     DhbInput::User(txns),
                            // ));


                            // android fix
                            // let is_empty_ = is_empty();
                            // if !is_empty_ {
                                let txns = gen_txns();
                                warn!("!! send_internal {} - {}", self.num, *hdb.inner.addr);
                                hdb.send_internal (
                                    InternalMessage::hb_input (
                                        hdb.inner.uid,
                                        OutAddr(*hdb.inner.addr),
                                        DhbInput::User(txns)
                                    )
                                );
                            // }
                        }
                        _ => {}
                    }
                    Ok(())
                })
                .map_err(|err| panic!("Contribution generation error: {:?}", err));

            Either::A(gen_cntrb)

        } else {
            Either::B(future::ok(()))
        }
    }

    /// Returns a future that generates random transactions and logs status
    /// messages.
    fn log_status(self) -> impl Future<Item = (), Error = ()> {
        Interval::new(
            Instant::now(),
            Duration::from_millis(self.inner.config.txn_gen_interval),
        )
        .for_each(move |_| {
            let hdb = self.clone();
            let peers = hdb.peers();

            // Log state:
            let (dsct, p_ttl, p_est) = hdb.state_info_stale();
            let peer_count = peers.count_total();
            info!("!! {} Hydrabadger State: {:?}({})", self.num, dsct, peer_count);

            // Log peer list:
            let peer_list = peers
                .peers()
                .map(|p| {
                    p.in_addr()
                        .map(|ia| ia.0.to_string())
                        .unwrap_or(format!("No in address"))
                })
                .collect::<Vec<_>>();
            info!("!! {}  Peers: {:?}", self.num, peer_list);

            // Log (trace) full peerhandler details:
            trace!("PeerHandler list:");
            for (peer_addr, _peer) in peers.iter() {
                trace!(" !! {}  peer_addr: {}", self.num, peer_addr);
            }

            drop(peers);

            Ok(())
        })
        .map_err(|err| panic!("List connection interval error: {:?}", err))
    }


    /// Binds to a host address and returns a future which starts the node.
    pub fn node(
        self,
        remotes: Option<HashSet<SocketAddr>>,
        gen_txns: Option<fn() -> T>,
        is_empty: fn() -> bool,
    ) -> impl Future<Item = (), Error = ()> {
        let socket = TcpListener::bind(&self.inner.addr).unwrap();
        info!("Listening on: {}", self.inner.addr);

        let remotes = remotes.unwrap_or(HashSet::new());

        let hdb = self.clone();
        let listen = socket
            .incoming()
            .map_err(|err| error!("Error accepting socket: {:?}", err))
            .for_each(move |socket| {
                tokio::spawn(hdb.clone().handle_incoming(socket));
                Ok(())
            });

        let hdb = self.clone();
        let local_pk = hdb.inner.secret_key.public_key();
        let connect = future::lazy(move || {
            for &remote_addr in remotes.iter().filter(|&&ra| ra != hdb.inner.addr.0) {
                if remote_addr != hdb.inner.addr_out {
                    tokio::spawn(
                        hdb.clone()
                            .connect_outgoing(remote_addr, local_pk, None, true),
                    );
                }
            }
            Ok(())
        });

        let hdb_handler = self
            .handler()
            .map_err(|err| error!("Handler internal error: {:?}", err));

        let log_status = self.clone().log_status();
        let generate_contributions = self.clone().generate_contributions(gen_txns, is_empty);

        listen
            .join5(connect, hdb_handler, log_status, generate_contributions)
            .map(|(..)| ())
    }

    /// Starts a node.
    pub fn run_node(
        self,
        remotes: Option<HashSet<SocketAddr>>,
        gen_txns: Option<fn() -> T>,
        is_empty: fn() -> bool,
    ) {
        tokio::run(self.node(remotes, gen_txns, is_empty));
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

    pub fn to_weak(&self) -> HydrabadgerWeak<T> {
        HydrabadgerWeak {
            inner: Arc::downgrade(&self.inner),
            handler: Arc::downgrade(&self.handler),
            batch_rx: Arc::downgrade(&self.batch_rx),
        }
    }
}

pub struct HydrabadgerWeak<T: Contribution> {
    inner: Weak<Inner<T>>,
    handler: Weak<Mutex<Option<Handler<T>>>>,
    batch_rx: Weak<Mutex<Option<BatchRx<T>>>>,
}

// impl<T: Contribution> HydrabadgerWeak<T> {
//     pub fn upgrade(self) -> Option<Hydrabadger<T>> {
//         self.inner.upgrade() .and_then(|inner| {
//             self.handler.upgrade().and_then(|handler| {
//                 self.batch_rx.upgrade().and_then(|batch_rx|{
//                     Some(Hydrabadger { inner, handler, batch_rx })
//                 })
//             })
//         })
//     }
// }