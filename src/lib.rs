#[cfg(target_os = "android")]
extern crate android_logger;
#[macro_use]
extern crate log;
extern crate log_panics;

#[cfg(target_os = "android")]
mod android_c_headers;
#[cfg(target_os = "android")]
pub mod java_glue;

#[cfg(feature = "nightly")]
extern crate alloc_system;
extern crate clap;

extern crate env_logger;
#[macro_use]
extern crate failure;
extern crate crossbeam;
// #[macro_use] extern crate crossbeam_channel;
extern crate crypto;
extern crate chrono;
extern crate num_traits;
extern crate num_bigint;
#[macro_use]
extern crate futures;
extern crate tokio;
extern crate tokio_codec;
extern crate tokio_io;
extern crate rand;
extern crate bytes;
extern crate uuid;
extern crate byteorder;
#[macro_use]
extern crate serde_derive;
extern crate serde;
extern crate serde_bytes;
extern crate bincode;
extern crate tokio_serde_bincode;
extern crate parking_lot;
extern crate clear_on_drop;
extern crate hbbft;

use android_logger::Filter;
use log::Level;
use parking_lot::{Mutex};


#[cfg(feature = "nightly")]
use alloc_system::System;

#[cfg(feature = "nightly")]
#[global_allocator]
static A: System = System;

pub mod hydrabadger;
pub mod blockchain;
pub mod peer;


use std::{
    collections::BTreeMap,
    fmt::{self},
    net::{SocketAddr},
    ops::Deref,
    sync::{
        Arc,
    },
    thread,
    mem,
};


use futures::{
    StartSend, AsyncSink,
    sync::mpsc,
};

use tokio::{
    io,
    net::{TcpStream},
    prelude::*,
};
use tokio_io::codec::length_delimited::Framed;
use bytes::{BytesMut, Bytes};
use rand::{Rng, Rand};
use uuid::Uuid;
// use bincode::{serialize, deserialize};
use hbbft::{
    crypto::{PublicKey, PublicKeySet},
    sync_key_gen::{Part, Ack},
    messaging::Step as MessagingStep,
    dynamic_honey_badger::{Message as DhbMessage, JoinPlan},
    queueing_honey_badger::{QueueingHoneyBadger, Input as QhbInput},
};

pub use hydrabadger::{Hydrabadger, Config};
pub use blockchain::{Blockchain, MiningError};

// FIME: TEMPORARY -- Create another error type.
pub use hydrabadger::{Error};


/// Transmit half of the wire message channel.
// TODO: Use a bounded tx/rx (find a sensible upper bound):
type WireTx = mpsc::UnboundedSender<WireMessage>;

/// Receive half of the wire message channel.
// TODO: Use a bounded tx/rx (find a sensible upper bound):
type WireRx = mpsc::UnboundedReceiver<WireMessage>;

/// Transmit half of the internal message channel.
// TODO: Use a bounded tx/rx (find a sensible upper bound):
type InternalTx = mpsc::UnboundedSender<InternalMessage>;

/// Receive half of the internal message channel.
// TODO: Use a bounded tx/rx (find a sensible upper bound):
type InternalRx = mpsc::UnboundedReceiver<InternalMessage>;


/// A transaction.
#[derive(Serialize, Deserialize, Eq, PartialEq, Hash, Ord, PartialOrd, Debug, Clone)]
pub struct Transaction(pub String);

static mut M_TEXT1: Option<String> = None;
static mut M_TEXT2: Option<String> = None;
static mut M_TEXT3: Option<String> = None;

impl Transaction {
    fn random(len: usize) -> Transaction {
        let consonants = "bcdfghjk lmnpqrstvwxyz ";
        let mut result = String::new();

        for _i in 0..len  {
            result.push(rand::sample(&mut rand::thread_rng(), consonants.chars(), 1)[0]);
        }

        Transaction(result)
    }

    fn get_tr1() -> Vec<Transaction> {
        unsafe {
            // warn!("!!get_tr1 {}", M_TEXT1 );
            let mut vec: Vec<Transaction> = Vec::new();
            match M_TEXT1 {
                Some(ref mut x) => {
                    warn!("!!get_tr1  {:?}", x);
                    vec.push( Transaction(x.to_string()) );
                    warn!("!!get_tr1 vec {:?}", vec);
                    M_TEXT1 = None;
                    vec
                }
                None => {
                    warn!("!!get_tr1 None");
                    vec
                }
            }
        }
    }

    fn get_tr2() -> Vec<Transaction> {
        unsafe {
            // warn!("!!get_tr2 {}", M_TEXT2);
            let mut vec: Vec<Transaction> = Vec::new();
            match M_TEXT2 {
                Some(ref mut x) => {
                    warn!("!!get_tr2  {:?}", x);
                    vec.push( Transaction(x.to_string()) );
                    warn!("!!get_tr2 vec {:?}", vec);
                    M_TEXT2 = None;
                    vec
                }
                None => {
                    warn!("!!get_tr2 None");
                    vec
                }
            }
        }
    }

    fn get_tr3() -> Vec<Transaction> {
        // warn!("!!get_tr3 {}", M_TEXT3);
        unsafe {
            let mut vec: Vec<Transaction> = Vec::new();
            match M_TEXT3 {
                Some(ref mut x) => {
                    warn!("!!get_tr3  {:?}", x);
                    vec.push( Transaction(x.to_string()) );
                    warn!("!!get_tr3 vec {:?}", vec);
                    M_TEXT3 = None;
                    vec
                }
                None => {
                    warn!("!!get_tr3 None");
                    vec
                }
            }
        }
    }
}


/// A unique identifier.
#[derive(Clone, Copy, Eq, Hash, Ord, PartialEq, PartialOrd, Serialize, Deserialize)]
pub struct Uid(pub(crate) Uuid);

impl Uid {
    /// Returns a new, random `Uid`.
    pub fn new() -> Uid {
        Uid(Uuid::new_v4())
    }
}

impl Rand for Uid {
    fn rand<R: Rng>(_rng: &mut R) -> Uid {
        Uid::new()
    }
}

impl fmt::Display for Uid {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        fmt::LowerHex::fmt(&self.0, f)
    }
}

impl fmt::Debug for Uid {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        fmt::LowerHex::fmt(&self.0, f)
    }
}

type Message = DhbMessage<Uid>;
type Step = MessagingStep<QueueingHoneyBadger<Vec<Transaction>, Uid>>;
type Input = QhbInput<Vec<Transaction>, Uid>;

/// A peer's incoming (listening) address.
#[derive(Clone, Copy, Debug, Serialize, Deserialize, PartialEq, Eq, Hash)]
pub struct InAddr(pub SocketAddr);

impl Deref for InAddr {
    type Target = SocketAddr;
    fn deref(&self) -> &SocketAddr {
        &self.0
    }
}

impl fmt::Display for InAddr {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "InAddr({})", self.0)
    }
}


/// An internal address used to respond to a connected peer.
#[derive(Clone, Copy, Debug, Serialize, Deserialize, PartialEq, Eq, Hash)]
pub struct OutAddr(pub SocketAddr);

impl Deref for OutAddr {
    type Target = SocketAddr;
    fn deref(&self) -> &SocketAddr {
        &self.0
    }
}

impl fmt::Display for OutAddr {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "OutAddr({})", self.0)
    }
}


/// Nodes of the network.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct NetworkNodeInfo {
    pub(crate) uid: Uid,
    pub(crate) in_addr: InAddr,
    pub(crate) pk: PublicKey,
}


/// The current state of the network.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub enum NetworkState {
    None,
    Unknown(Vec<NetworkNodeInfo>),
    AwaitingMorePeersForKeyGeneration(Vec<NetworkNodeInfo>),
    GeneratingKeys(Vec<NetworkNodeInfo>, BTreeMap<Uid, PublicKey>),
    Active((Vec<NetworkNodeInfo>, PublicKeySet, BTreeMap<Uid, PublicKey>)),
}


/// Messages sent over the network between nodes.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub enum WireMessageKind {
    HelloFromValidator(Uid, InAddr, PublicKey, NetworkState),
    HelloRequestChangeAdd(Uid, InAddr, PublicKey),
    WelcomeReceivedChangeAdd(Uid, PublicKey, NetworkState),
    RequestNetworkState,
    NetworkState(NetworkState),
    Goodbye,
    #[serde(with = "serde_bytes")]
    Bytes(Bytes),
    Message(Uid, Message),
    Transactions(Uid, Vec<Transaction>),
    KeyGenPart(Part),
    KeyGenAck(Ack),
    JoinPlan(JoinPlan<Uid>)
    // TargetedMessage(TargetedMessage<Uid>),
}


/// Messages sent over the network between nodes.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct WireMessage {
    kind: WireMessageKind,
}

impl WireMessage {
    pub fn hello_from_validator(src_uid: Uid, in_addr: InAddr, pk: PublicKey,
            net_state: NetworkState) -> WireMessage {
        WireMessageKind::HelloFromValidator(src_uid, in_addr, pk, net_state).into()
    }

    /// Returns a `HelloRequestChangeAdd` variant.
    pub fn hello_request_change_add(src_uid: Uid, in_addr: InAddr, pk: PublicKey) -> WireMessage {
        WireMessage { kind: WireMessageKind::HelloRequestChangeAdd(src_uid, in_addr, pk), }
    }

    /// Returns a `WelcomeReceivedChangeAdd` variant.
    pub fn welcome_received_change_add(src_uid: Uid, pk: PublicKey, net_state: NetworkState)
            -> WireMessage {
        WireMessage { kind: WireMessageKind::WelcomeReceivedChangeAdd(src_uid, pk, net_state) }
    }

    /// Returns an `Input` variant.
    pub fn transaction(src_uid: Uid, txns: Vec<Transaction>) -> WireMessage {
        WireMessage { kind: WireMessageKind::Transactions(src_uid, txns), }
    }

    /// Returns a `Message` variant.
    pub fn message(src_uid: Uid, msg: Message) -> WireMessage {
        WireMessage { kind: WireMessageKind::Message(src_uid, msg), }
    }

    pub fn key_gen_part(part: Part) -> WireMessage {
        WireMessage { kind: WireMessageKind::KeyGenPart(part) }
    }

    pub fn key_gen_part_ack(outcome: Ack) -> WireMessage {
        WireMessageKind::KeyGenAck(outcome).into()
    }

    pub fn join_plan(jp: JoinPlan<Uid>) -> WireMessage {
        WireMessageKind::JoinPlan(jp).into()
    }

    /// Returns the wire message kind.
    pub fn kind(&self) -> &WireMessageKind {
        &self.kind
    }

    /// Consumes this `WireMessage` into its kind.
    pub fn into_kind(self) -> WireMessageKind {
        self.kind
    }
}

impl From<WireMessageKind> for WireMessage {
    fn from(kind: WireMessageKind) -> WireMessage {
        WireMessage { kind }
    }
}



/// A stream/sink of `WireMessage`s connected to a socket.
#[derive(Debug)]
pub struct WireMessages {
    framed: Framed<TcpStream>,
}

impl WireMessages {
    pub fn new(socket: TcpStream) -> WireMessages {
        WireMessages {
            framed: Framed::new(socket),
        }
    }

    pub fn socket(&self) -> &TcpStream {
        self.framed.get_ref()
    }

    pub fn send_msg(&mut self, msg: WireMessage) -> Result<(), Error> {
        self.start_send(msg)?;
        let _ = self.poll_complete()?;
        Ok(())
    }
}

impl Stream for WireMessages {
    type Item = WireMessage;
    type Error = Error;

    fn poll(&mut self) -> Poll<Option<Self::Item>, Self::Error> {
        match try_ready!(self.framed.poll()) {
            Some(frame) => {
                Ok(Async::Ready(Some(
                    // deserialize_from(frame.reader()).map_err(Error::Serde)?
                    bincode::deserialize(&frame.freeze()).map_err(Error::Serde)?
                )))
            }
            None => Ok(Async::Ready(None))
        }
    }
}

impl Sink for WireMessages {
    type SinkItem = WireMessage;
    type SinkError = Error;

    fn start_send(&mut self, item: Self::SinkItem) -> StartSend<Self::SinkItem, Self::SinkError> {
        // TODO: Reuse buffer:
        let mut serialized = BytesMut::new();

        // Downgraded from bincode 1.0:
        //
        // Original: `bincode::serialize(&item)`
        //
        match bincode::serialize(&item, bincode::Bounded(1 << 20)) {
            Ok(s) => serialized.extend_from_slice(&s),
            Err(err) => return Err(Error::Io(io::Error::new(io::ErrorKind::Other, err))),
        }
        match self.framed.start_send(serialized) {
            Ok(async_sink) => match async_sink {
                AsyncSink::Ready => Ok(AsyncSink::Ready),
                AsyncSink::NotReady(_) => Ok(AsyncSink::NotReady(item)),
            },
            Err(err) => Err(Error::Io(err))
        }
    }

    fn poll_complete(&mut self) -> Poll<(), Self::SinkError> {
        self.framed.poll_complete().map_err(Error::from)
    }

    fn close(&mut self) -> Poll<(), Self::SinkError> {
        self.framed.close().map_err(Error::from)
    }
}



/// A message between internal threads/tasks.
#[derive(Clone, Debug)]
pub enum InternalMessageKind {
    Wire(WireMessage),
    HbMessage(Message),
    HbInput(Input),
    PeerDisconnect,
    NewIncomingConnection(InAddr, PublicKey, bool),
    NewOutgoingConnection,
}


/// A message between internal threads/tasks.
#[derive(Clone, Debug)]
pub struct InternalMessage {
    src_uid: Option<Uid>,
    src_addr: OutAddr,
    kind: InternalMessageKind,
}

impl InternalMessage {
    pub fn new(src_uid: Option<Uid>, src_addr: OutAddr, kind: InternalMessageKind) -> InternalMessage {
        InternalMessage { src_uid: src_uid, src_addr, kind }
    }

    /// Returns a new `InternalMessage` without a uid.
    pub fn new_without_uid(src_addr: OutAddr, kind: InternalMessageKind) -> InternalMessage {
        InternalMessage::new(None, src_addr, kind)
    }

    pub fn wire(src_uid: Option<Uid>, src_addr: OutAddr, wire_message: WireMessage) -> InternalMessage {
        InternalMessage::new(src_uid, src_addr, InternalMessageKind::Wire(wire_message))
    }

    pub fn hb_message(src_uid: Uid, src_addr: OutAddr, msg: Message) -> InternalMessage {
        InternalMessage::new(Some(src_uid), src_addr, InternalMessageKind::HbMessage(msg))
    }

    pub fn hb_input(src_uid: Uid, src_addr: OutAddr, input: Input) -> InternalMessage {
        InternalMessage::new(Some(src_uid), src_addr, InternalMessageKind::HbInput(input))
    }

    pub fn peer_disconnect(src_uid: Uid, src_addr: OutAddr) -> InternalMessage {
        InternalMessage::new(Some(src_uid), src_addr, InternalMessageKind::PeerDisconnect)
    }

    pub fn new_incoming_connection(src_uid: Uid, src_addr: OutAddr, src_in_addr: InAddr,
            src_pk: PublicKey, request_change_add: bool) -> InternalMessage {
        InternalMessage::new(Some(src_uid), src_addr,
            InternalMessageKind::NewIncomingConnection(src_in_addr, src_pk, request_change_add))
    }

    pub fn new_outgoing_connection(src_addr: OutAddr) -> InternalMessage {
        InternalMessage::new_without_uid(src_addr, InternalMessageKind::NewOutgoingConnection)
    }

    /// Returns the source unique identifier this message was received in.
    pub fn src_uid(&self) -> Option<&Uid> {
        self.src_uid.as_ref()
    }

    /// Returns the source socket this message was received on.
    pub fn src_addr(&self) -> &OutAddr {
        &self.src_addr
    }

    /// Returns the internal message kind.
    pub fn kind(&self) -> &InternalMessageKind {
        &self.kind
    }

    /// Consumes this `InternalMessage` into its parts.
    pub fn into_parts(self) -> (Option<Uid>, OutAddr, InternalMessageKind) {
        (self.src_uid, self.src_addr, self.kind)
    }
}









use std::collections::HashSet;

trait OnEvent {
    fn changed(&self, its_me: bool, id: String, trans: String);
}

fn callback(num_call_back: i32, its_me: bool, id: String, trans: String) {
    unsafe {
        match M_SESSION_PTR {
            Some(ref mut x) => x.change(num_call_back, its_me, id, trans),
            None => panic!(),
        } 
    }
}

static mut M_SESSION_PTR: Option<&'static mut Session> = None;
static mut M_NUM_OF_CALLBACK: i32 = 0;

struct Session {
    observers: Vec<Box<OnEvent>>,
    handler1: Arc<Mutex<Option<Hydrabadger>>>,
    handler2: Arc<Mutex<Option<Hydrabadger>>>,
    handler3: Arc<Mutex<Option<Hydrabadger>>>,
}

impl Session {
    pub fn new() -> Session {
        android_logger::init_once(
            Filter::default()
                .with_min_level(Level::Trace), // limit log level
            Some("HYDRABADGERTAG") // logs will show under mytag tag. If `None`, the crate name will be used
        ); 
           
        log_panics::init(); // log panics rather than printing them
        info!("init log system - done");

        Session {  observers: Vec::new(),
                    handler1: Arc::new(Mutex::new(None)),
                    handler2: Arc::new(Mutex::new(None)),
                    handler3: Arc::new(Mutex::new(None)),}
    }

    fn subscribe(&mut self, cb: Box<OnEvent>) {
        self.observers.push(cb);
    }

    pub fn after_subscribe(&'static mut self) {
        unsafe {
            M_SESSION_PTR = Some(self);
        }
    }

    pub fn send_message(&self, num: i32, str1: String) {
        unsafe {
            warn!("!!send_message: {:?}", str1);

            let mut new_string = String::new();
            new_string = format!("{}!", str1);


            // let ret = mem::transmute(&str1 as &String);
            // mem::forget(str1);
            // let S1: &'static mut String = ret;
            warn!("!!send_message string: {:?}", new_string);

            if num == 0 {
                M_TEXT1 = Some(new_string);
                warn!("!!send_message string1: {:?}", M_TEXT1);
            }
            else if num == 1 {
                M_TEXT2 = Some(new_string);
                warn!("!!send_message string1: {:?}", M_TEXT2);
            }
            else if num == 2 {
                M_TEXT3 = Some(new_string);
                warn!("!!send_message string1: {:?}", M_TEXT3);
            }
        }
    }

    pub fn change(&self, x: i32, its_me: bool, id: String, trans: String) {
        let mut i = 0;
        for cb in &self.observers {
            if i == x {
                warn!("Call callback at number: {:?}", i);
                cb.changed(its_me, id.clone(), trans.clone());
            }
            i += 1;
        }
    }

    pub fn start_node(&self, ipport_string_source: String, ipport_string_myout: String, ipports_string_remote: String) {
        unsafe {
            warn!("enter to startNode: {:?}", M_NUM_OF_CALLBACK.clone());
        }

        let bind_address: SocketAddr = ipport_string_source.parse().expect("Unable to parse socket address");
        let bind_address_out: SocketAddr = ipport_string_myout.parse().expect("Unable to parse socket address");
    
        let mut remote_addresses: HashSet<SocketAddr> = HashSet::new();
        let split = ipports_string_remote.split(";");
        for address in split {
            remote_addresses.insert(address.parse().expect("Unable to parse socket address"));
        }

        let cfg = Config::default();
         
        let callback_ = callback;

        unsafe {
            let num = M_NUM_OF_CALLBACK.clone();
            // let hb = Hydrabadger::new(bind_address, cfg, callback_, num);

            if num == 0 {
                *self.handler1.lock() = Some(Hydrabadger::new(bind_address, bind_address_out, cfg, callback_, num));
                M_NUM_OF_CALLBACK += 1;
                
                match self.handler1.lock().take() {
                    Some(v) => {
                        thread::spawn(move || {
                            v.run_node(Some(remote_addresses));
                        });
                    },
                    None => {},
                }
                warn!("!!match out started Node: {:?}", num);
            }
            else if num == 1 {
                *self.handler2.lock() = Some(Hydrabadger::new(bind_address, bind_address_out, cfg, callback_, num));
                M_NUM_OF_CALLBACK += 1;
                match self.handler2.lock().take() {
                    Some(v) => {
                        thread::spawn(move || {
                            v.run_node(Some(remote_addresses));
                        });
                    },
                    None => {},
                }
                warn!("!!match out started Node: {:?}", num);
            }
            else if num == 2 {
                *self.handler3.lock() = Some(Hydrabadger::new(bind_address, bind_address_out, cfg, callback_, num));
                M_NUM_OF_CALLBACK += 1;

                match self.handler3.lock().take() {
                    Some(v) => {
                        thread::spawn(move || {
                            v.run_node(Some(remote_addresses));
                        });
                    },
                    None => {},
                }
                warn!("!!match out started Node: {:?}", num);
            }
        }
    }

}
