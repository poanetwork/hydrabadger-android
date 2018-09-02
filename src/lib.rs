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


#[cfg(feature = "nightly")]
use alloc_system::System;

#[cfg(feature = "nightly")]
#[global_allocator]
static A: System = System;

// pub mod network;
pub mod hydrabadger;
pub mod blockchain;
pub mod peer;



use std::{
    collections::BTreeMap,
    fmt::{self},
    net::{SocketAddr},
    ops::Deref,
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
pub struct Transaction(pub Vec<u8>);

impl Transaction {
    fn random(len: usize) -> Transaction {
        Transaction(rand::thread_rng().gen_iter().take(len).collect())
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
use std::net::Ipv4Addr;
use std::net::IpAddr;


trait OnEvent1 {
    fn node_started_1(&self, start: bool);
}

trait OnEvent2 {
    fn node_started_2(&self, start: bool);
}

trait OnEvent3 {
    fn node_started_3(&self, start: bool);
}


fn simple_callback1() {
    unsafe {
        match M_SESSION_PTR {
            Some(ref mut x) => x.change1(true),
            None => panic!(),
        } 
    }
}
fn simple_callback2() {
    unsafe {
        match M_SESSION_PTR {
            Some(ref mut x) => x.change2(true),
            None => panic!(),
        } 
    }
}
fn simple_callback3() {
    unsafe {
        match M_SESSION_PTR {
            Some(ref mut x) => x.change3(true),
            None => panic!(),
        } 
    }
}

static mut M_SESSION_PTR: Option<&'static mut Session> = None;



struct Session {
    observers1:   Vec<Box<OnEvent1>>,
    observers2:   Vec<Box<OnEvent2>>,
    observers3:   Vec<Box<OnEvent3>>,
    a: i32,
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

        Session { observers1: Vec::new(), 
                    observers2: Vec::new(), 
                    observers3: Vec::new(), 
                    a: 2 }
    }

    pub fn add_and1(&self, val: i32) -> i32 {
        self.a + val + 1
    }

    // Greeting with full, no-runtime-cost support for newlines and UTF-8
    pub fn greet(to: &str) -> String {
        format!("Hello {} ✋\nIt's a pleasure to meet you!", to)
    }

    pub fn after_subscribe(&'static mut self) {
        unsafe {
            M_SESSION_PTR = Some(self);
        }
    }

    fn subscribe1(&mut self, cb: Box<OnEvent1>) {
        self.observers1.push(cb);
    }

    fn subscribe2(&mut self, cb: Box<OnEvent2>) {
        self.observers2.push(cb);
    }

    fn subscribe3(&mut self, cb: Box<OnEvent3>) {
        self.observers3.push(cb);
    }

    pub fn change1 (&self, x: bool) {
        for cb in &self.observers1 {
            cb.node_started_1(x);
        }
    }

    pub fn change2 (&self, x: bool) {
        for cb in &self.observers2 {
            cb.node_started_2(x);
        }
    }

    pub fn change3 (&self, x: bool) {
        for cb in &self.observers3 {
            cb.node_started_3(x);
        }
    }



    pub fn start_node1(&self) {
        warn!("enter to startNode1");
        let bind_address: SocketAddr = SocketAddr::new(IpAddr::V4(Ipv4Addr::new(127, 0, 0, 1)), 3000);
    
        let mut remote_addresses: HashSet<SocketAddr> = HashSet::new();
        remote_addresses.insert(SocketAddr::new(IpAddr::V4(Ipv4Addr::new(127, 0, 0, 1)), 3001));
        remote_addresses.insert(SocketAddr::new(IpAddr::V4(Ipv4Addr::new(127, 0, 0, 1)), 3002));

        let cfg = Config::default();

        let callbackbatch = simple_callback1;

        let hb = Hydrabadger::new(bind_address, cfg, callbackbatch);
        warn!("Hydrabadger::new");
        hb.run_node(Some(remote_addresses));
        warn!("startNode1");
    }

    pub fn start_node2(&self) {
        warn!("enter to startNode2");
        let bind_address: SocketAddr = SocketAddr::new(IpAddr::V4(Ipv4Addr::new(127, 0, 0, 1)), 3001);
    
        let mut remote_addresses: HashSet<SocketAddr> = HashSet::new();
        remote_addresses.insert(SocketAddr::new(IpAddr::V4(Ipv4Addr::new(127, 0, 0, 1)), 3000));
        remote_addresses.insert(SocketAddr::new(IpAddr::V4(Ipv4Addr::new(127, 0, 0, 1)), 3002));

        let cfg = Config::default();

        let callbackbatch = simple_callback2;

        let hb = Hydrabadger::new(bind_address, cfg,  callbackbatch);
        hb.run_node(Some(remote_addresses));
        warn!("startNode2");
    }


    pub fn start_node3(&self) {
        warn!("enter to startNode3");
        let bind_address: SocketAddr = SocketAddr::new(IpAddr::V4(Ipv4Addr::new(127, 0, 0, 1)), 3002);
    
        let mut remote_addresses: HashSet<SocketAddr> = HashSet::new();
        remote_addresses.insert(SocketAddr::new(IpAddr::V4(Ipv4Addr::new(127, 0, 0, 1)), 3001));
        remote_addresses.insert(SocketAddr::new(IpAddr::V4(Ipv4Addr::new(127, 0, 0, 1)), 3000));

        let cfg = Config::default();

        let callbackbatch = simple_callback3;

        let hb = Hydrabadger::new(bind_address, cfg,  callbackbatch);
        hb.run_node(Some(remote_addresses));
        warn!("startNode3");
    }


}
