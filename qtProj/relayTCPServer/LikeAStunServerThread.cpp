#include "LikeAStunServerThread.h"

#include <QtNetwork>
#include <memory>
#include <QAbstractSocket>

//! [0]
LikeAStunServerThread::LikeAStunServerThread(qintptr socketDescriptor, QObject *parent)
    : QThread(parent), socketDescriptor(socketDescriptor), mStopThread(false)
{
}

LikeAStunServerThread::~LikeAStunServerThread()
{
    setStopThread(true);
    QThread::wait();
}
//! [0]

void LikeAStunServerThread::waitForByte(QTcpSocket &socket, int size)
{
    while(!mStopThread && socket.bytesAvailable() < size){
        socket.waitForReadyRead();
        msleep(10);
    }
}

void LikeAStunServerThread::displayError(QAbstractSocket::SocketError socketError)
{
    switch (socketError) {
    case QAbstractSocket::ConnectionRefusedError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"The connection was refused by the peer (or timed out).";
                break;
    case QAbstractSocket::RemoteHostClosedError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"The remote host closed the connection. Note that the client socket (i.e., this socket) will be closed after the remote close notification has been sent.";
                break;
    case QAbstractSocket::HostNotFoundError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"The host address was not found.";
                break;
    case QAbstractSocket::SocketAccessError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"The socket operation failed because the application lacked the required privileges.";
                break;
    case QAbstractSocket::SocketResourceError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"The local system ran out of resources (e.g., too many sockets).";
                break;
    case QAbstractSocket::SocketTimeoutError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"The socket operation timed out.";
                break;
    case QAbstractSocket::DatagramTooLargeError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"The datagram was larger than the operating system's limit (which can be as low as 8192 bytes).";
                break;
    case QAbstractSocket::NetworkError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"An error occurred with the network (e.g., the network cable was accidentally plugged out).";
                break;
    case QAbstractSocket::AddressInUseError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"The address specified to QAbstractSocket::bind() is already in use and was set to be exclusive.";
                break;
    case QAbstractSocket::SocketAddressNotAvailableError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"The address specified to QAbstractSocket::bind() does not belong to the host.";
                break;
    case QAbstractSocket::UnsupportedSocketOperationError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"The requested socket operation is not supported by the local operating system (e.g., lack of IPv6 support).";
                break;
    case QAbstractSocket::UnknownSocketError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"An unidentified error occurred.";
                break;
    default:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"An unidentified error occurred. 1";
    }
}

bool LikeAStunServerThread::StopThread() const
{
    return mStopThread;
}

void LikeAStunServerThread::setStopThread(bool StopThread)
{
    if (mStopThread == StopThread)
        return;

    mStopThread = StopThread;
    emit StopThreadChanged(mStopThread);
}
//! [4]

void LikeAStunServerThread::beenInitedHandle(Ports port)
{
    this->ports = port;
    mIsinitHandle = true;
}

//! [1]
void LikeAStunServerThread::run()
{
    QTcpSocket tcpSocket;
    connect(&tcpSocket, QOverload<QTcpSocket::SocketError>::of(&QTcpSocket::error),
            this, &LikeAStunServerThread::displayError);

    if (!tcpSocket.setSocketDescriptor(socketDescriptor)) {
        return;
    }

    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Income connection from "<<tcpSocket.peerAddress().toString()<<" IP "<<tcpSocket.peerPort()<< " PORT "<<" - Thread "<<this->currentThreadId();
    in.setDevice(&tcpSocket);

    if (!tcpSocket.waitForReadyRead()) {
        qCritical()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"LikeAStunServerThread return error from "<<tcpSocket.peerAddress().toString()<<" IP "<<tcpSocket.peerPort()<< " PORT error - "<<tcpSocket.errorString()<<" - Thread "<<this->currentThreadId();

        QByteArray block;
        QDataStream out(&block, QIODevice::WriteOnly);
        out << QString("ERROR");
        tcpSocket.write(block);
        tcpSocket.disconnectFromHost();
        tcpSocket.waitForDisconnected();
        return;
    }

    //READ MAGIC NUMBER
    waitForByte(tcpSocket, sizeof (quint32));
    quint32 ar;
    in >> ar;
    //CHECK MAGIC NUMBER
    if(MAGIC_NUMBER != ar) {
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"MAGIC NOT DONE  for "<<tcpSocket.peerAddress().toString()<<" IP "<<tcpSocket.peerPort()<< " PORT "<<" - Thread "<<this->currentThreadId();

        QByteArray block;
        QDataStream out(&block, QIODevice::WriteOnly);
        out << QString("ERROR");
        tcpSocket.write(block);
        tcpSocket.disconnectFromHost();
        tcpSocket.waitForDisconnected();
        return;
    }

    // get size
    waitForByte(tcpSocket, sizeof (qint32));
    qint32 sizeOfStringArray = 0;
    in >> sizeOfStringArray;
    // get UID
    waitForByte(tcpSocket, sizeOfStringArray);
    QByteArray uid;
    uid.resize(sizeOfStringArray);
    in.readRawData(uid.data(), sizeOfStringArray);
    QString UID = QString::fromUtf8(uid.data(), sizeOfStringArray);
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"UID "<<uid<<" - Thread "<<this->currentThreadId();
    // get time
    QTime time = QTime::currentTime();

    ////Event LOOP
    ports = Ports(-1,-1);
    mIsinitHandle = false;
    emit initHandle(UID, tcpSocket.peerAddress().toString(),
                    tcpSocket.peerPort(), (quint64)((quint64)time.msecsSinceStartOfDay()/(quint64)1000));
    while(!mIsinitHandle && !mStopThread) {
        msleep(1);
    }
    mIsinitHandle = false;

    if(ports == Ports(-1,-1)) {
        qCritical()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"LikeAStunServerThread return ERROR - Thread "<<this->currentThreadId();

        QByteArray block;
        QDataStream out(&block, QIODevice::WriteOnly);
        out << QString("ERROR");
        tcpSocket.write(block);
        tcpSocket.disconnectFromHost();
        tcpSocket.waitForDisconnected();
        return;
    }

    QByteArray block;
    QDataStream out(&block, QIODevice::WriteOnly);
    out << ports.first;
    out << ports.second;
    out << tcpSocket.peerPort();
    out << tcpSocket.peerAddress().toString().toLocal8Bit().size();
    out << tcpSocket.peerAddress().toString().toUtf8();

    tcpSocket.write(block);
    tcpSocket.disconnectFromHost();
    tcpSocket.waitForDisconnected();
}
