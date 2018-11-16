#include "StopServerThread.h"
#include "LikeAStunServerThread.h"

//quint32 MAGIC_NUMBER = (quint32)0xCAFECAFE;

StopServerThread::StopServerThread(qintptr socketDescriptor, QObject *parent)
    : QThread(parent), socketDescriptor(socketDescriptor), m_StopThread(false)
{
}

StopServerThread::~StopServerThread()
{
    setStopThread(true);
    QThread::wait();
}

bool StopServerThread::StopThread() const
{
    return m_StopThread;
}

void StopServerThread::setStopThread(bool StopThread)
{
    if (m_StopThread == StopThread)
        return;

    m_StopThread = StopThread;
    emit StopThreadChanged(m_StopThread);
}

void StopServerThread::displayError(QAbstractSocket::SocketError socketError)
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
void StopServerThread::waitForByte(QTcpSocket &socket, int size)
{
    while(!m_StopThread && socket.bytesAvailable() < size){
        socket.waitForReadyRead();
        msleep(10);
    }
}

void StopServerThread::setUnblock()
{
    mIsBlock = true;
}

void StopServerThread::run()
{
    QTcpSocket tcpSocket;
    connect(&tcpSocket, QOverload<QTcpSocket::SocketError>::of(&QTcpSocket::error),
            this, &StopServerThread::displayError);

    if (!tcpSocket.setSocketDescriptor(socketDescriptor)) {
        return;
    }

    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"StopServerThread Income connection from "<<tcpSocket.peerAddress().toString()<<" IP "<<tcpSocket.peerPort()<< " PORT "<<" - Thread "<<this->currentThreadId();
    in.setDevice(&tcpSocket);

    if (!tcpSocket.waitForReadyRead()) {
        qCritical()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"StopServerThread return ERROR to con from "<<tcpSocket.peerAddress().toString()<<" IP "<<tcpSocket.peerPort()<< " PORT error - "<<tcpSocket.errorString()<<" - Thread "<<this->currentThreadId();

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
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Stoping all with UID - "<<UID<<" - Thread "<<LikeAStunServerThread::currentThreadId();

    ////Event LOOP
    mIsBlock = false;
    emit stopHandleWithUID(UID);
    while(!mIsBlock && !m_StopThread) {
        msleep(1);
    }
    mIsBlock = false;

    QByteArray block;
    QDataStream out(&block, QIODevice::WriteOnly);
    out << QString("OK");

    tcpSocket.write(block);
    tcpSocket.disconnectFromHost();
    tcpSocket.waitForDisconnected();
}
