package net.korul.hbbft.P2P

import android.content.Context
import android.util.Log
import io.nats.client.Connection
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.*
import java.nio.charset.StandardCharsets
import java.util.*

class Connections() {
    private var TAG = "HYDRABADGERTAG:Connections"

    var peerConnection: PeerConnection? = null

    var dataChannel: DataChannel? = null

    var conNats: Connection? = null

    var publishTo: String? = null

    var myName: String? = null

    private val iceServers = LinkedList<PeerConnection.IceServer>()

    private var peerConnectionFactory: PeerConnectionFactory? = null

    private val constraints = MediaConstraints()

    var mInited = false

    var callback: IGetData? = null

    var mIamReadyToDataTranfer = false

    constructor(
        context: Context, publishto: String, myName_: String,
        conNats_: Connection, initiator: Boolean, callback_: IGetData
    ) : this() {
        publishTo = publishto
        myName = myName_
        conNats = conNats_
        callback = callback_

        iceServers.add(PeerConnection.IceServer("stun:stun.l.google.com:19302"))
        iceServers.add(PeerConnection.IceServer("stun:62.176.10.54:3478"))
        iceServers.add(PeerConnection.IceServer("turn:62.176.10.54:3478", "test1", "test1"))

        iceServers.add(PeerConnection.IceServer("stun:108.61.190.95:3478"))
        iceServers.add(PeerConnection.IceServer("turn:108.61.190.95:3478", "test1", "test1"))

        Log.d(
            TAG, if (PeerConnectionFactory.initializeAndroidGlobals(context, true, true, true))
                "Success initAndroidGlobals"
            else
                "Failed initAndroidGlobals"
        )

        peerConnectionFactory = PeerConnectionFactory()

        peerConnection = peerConnectionFactory!!.createPeerConnection(iceServers, constraints, PeerConnectionObserver)

        if (initiator) {
            StartConnect()
        }

        mInited = true
    }

    fun FreeConnect() {
        if (dataChannel != null) {
            dataChannel!!.close()
            dataChannel!!.unregisterObserver()
        }

        if (peerConnection != null)
            peerConnection!!.close()

        mInited = false
    }

    private fun StartConnect() {
        dataChannel = peerConnection!!.createDataChannel("RTCDataChannel", DataChannel.Init())
        dataChannel!!.registerObserver(DataChannelObserver)

        startConnectivity(constraints)
    }

    private fun startConnectivity(constraints: MediaConstraints) {
        peerConnection!!.createOffer(SessionObserver, constraints)
    }

    private var PeerConnectionObserver: PeerConnection.Observer = object : PeerConnection.Observer {
        override fun onSignalingChange(signalingState: PeerConnection.SignalingState) {
            Log.d(TAG, "PeerConnectionObserver onSignalingChange() " + signalingState.name)
        }

        override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState) {
            Log.d(TAG, "PeerConnectionObserver onIceConnectionChange() " + iceConnectionState.name)
        }

        override fun onIceConnectionReceivingChange(b: Boolean) {
            Log.d(TAG, "PeerConnectionObserver onIceConnectionReceivingChange(): $b")
        }

        override fun onIceGatheringChange(iceGatheringState: PeerConnection.IceGatheringState) {
            Log.d(TAG, "PeerConnectionObserver onIceGatheringChange() " + iceGatheringState.name)
        }

        override fun onIceCandidate(iceCandidate: IceCandidate) {
            Log.d(TAG, "PeerConnectionObserver onIceCandidate: " + iceCandidate.toString())

            val json = JSONObject()

            val message: String

            try {
                json.put("type", "candidate")
                json.put("UID", myName)
                json.put("toUser", publishTo)
                json.put("sdpMLineIndex", iceCandidate.sdpMLineIndex)
                json.put("sdpMid", iceCandidate.sdpMid)
                json.put("candidate", iceCandidate.sdp)

                message = json.toString()

                Log.d(TAG, "remote iceCandidateJson$message")

                conNats?.publish(publishTo, message.toByteArray(StandardCharsets.UTF_8))
                // Here, send a message to the other party in the WebSocket, etc.
                // On the receiving side,

//                val json2 = JSONObject(message)
//                val candidate = IceCandidate(json2.getString("sdpMid"), json2.getInt("sdpMLineIndex"), json2.getString("candidate"))
//                localPeerConnection!!.addIceCandidate(candidate)

            } catch (ex: org.json.JSONException) {
                Log.d(TAG, ex.toString())
            }

        }

        override fun onAddStream(mediaStream: MediaStream) {

        }

        override fun onRemoveStream(mediaStream: MediaStream) {

        }

        override fun onDataChannel(dataChannel: DataChannel) {
            Log.d(TAG, "PeerConnectionObserver onDataChannel()")
            this@Connections.dataChannel = dataChannel
            this@Connections.dataChannel!!.registerObserver(DataChannelObserver)
        }

        override fun onRenegotiationNeeded() {
            Log.d(TAG, "PeerConnectionObserver onRenegotiationNeeded()")
        }
    }

    var SessionObserver: SdpObserver = object : SdpObserver {
        override fun onCreateSuccess(sessionDescription: SessionDescription) {
            Log.d(TAG, "remote onCreateSuccess()")

            peerConnection!!.setLocalDescription(this, sessionDescription)

            val json = JSONObject()
            val message: String

            try {
                json.put("type", sessionDescription.type.toString().toLowerCase())
                json.put("UID", myName)
                json.put("toUser", publishTo)
                json.put("sdp", sessionDescription.description)

                message = json.toString()
                Log.d(TAG, message)

                conNats?.publish(publishTo, message.toByteArray(StandardCharsets.UTF_8))
                /**Signaling Mechanism to exchange SessionDescription object information goes here */

//                val json2 = JSONObject(message)
//                val type = json2.getString("type")
//                val sdp = json2.getString("sdp")
//
//                val sdp2 = SessionDescription(SessionDescription.Type.fromCanonicalForm(type), sdp)
//                localPeerConnection!!.setRemoteDescription(localSessionObserver, sdp2)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        override fun onSetSuccess() {
            Log.d(TAG, "remote onSetSuccess()")
        }

        override fun onCreateFailure(s: String) {
            Log.d(TAG, "remote onCreateFailure() $s")
        }

        override fun onSetFailure(s: String) {
            Log.d(TAG, "remote onSetFailure()")
        }
    }

    var DataChannelObserver: DataChannel.Observer = object : DataChannel.Observer {
        override fun onBufferedAmountChange(l: Long) {

        }

        override fun onStateChange() {
            Log.d(TAG, "remoteDataChannel onStateChange() " + dataChannel!!.state().name)

            if (dataChannel!!.state() == DataChannel.State.OPEN) {
                mIamReadyToDataTranfer = true
            }
        }

        override fun onMessage(buffer: DataChannel.Buffer) {
            Log.d(TAG, "onMessage ")

            if (buffer.binary) {
                val limit = buffer.data.limit()
                val data = ByteArray(limit)
                buffer.data.get(data)

                Log.d(TAG, "Reciver ${data.size} bytes")
                callback?.dataReceived(data)
            }
        }
    }
}