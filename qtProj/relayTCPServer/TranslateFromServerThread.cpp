#include "TranslateFromServerThread.h"

#include <utility>
#include <math.h>
#include <QDateTime>

TranslateFromServerThread::TranslateFromServerThread(qintptr socketDescriptor, QObject *parent)
    : QThread(parent), socketDescriptor(socketDescriptor), m_StopThread(false)
{
}

TranslateFromServerThread::~TranslateFromServerThread()
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<"Destructor TranslateFromServerThread";
    setStopThread(true);
    QThread::wait();
}

bool TranslateFromServerThread::StopThread() const
{
    return m_StopThread;
}

void TranslateFromServerThread::setStopThread(bool StopThread)
{
    if (m_StopThread == StopThread)
        return;

    m_StopThread = StopThread;
    emit StopThreadChanged(m_StopThread);
}


void TranslateFromServerThread::displayError(QAbstractSocket::SocketError socketError)
{
    switch (socketError) {
    case QAbstractSocket::ConnectionRefusedError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateFromServerThread The connection was refused by the peer (or timed out).";
                break;
    case QAbstractSocket::RemoteHostClosedError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateFromServerThread The remote host closed the connection. Note that the client socket (i.e., this socket) will be closed after the remote close notification has been sent.";
                break;
    case QAbstractSocket::HostNotFoundError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateFromServerThread The host address was not found.";
                break;
    case QAbstractSocket::SocketAccessError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateFromServerThread The socket operation failed because the application lacked the required privileges.";
                break;
    case QAbstractSocket::SocketResourceError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateFromServerThread The local system ran out of resources (e.g., too many sockets).";
                break;
    case QAbstractSocket::SocketTimeoutError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateFromServerThread The socket operation timed out.";
                break;
    case QAbstractSocket::DatagramTooLargeError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateFromServerThread The datagram was larger than the operating system's limit (which can be as low as 8192 bytes).";
                break;
    case QAbstractSocket::NetworkError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateFromServerThread An error occurred with the network (e.g., the network cable was accidentally plugged out).";
                break;
    case QAbstractSocket::AddressInUseError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateFromServerThread The address specified to QAbstractSocket::bind() is already in use and was set to be exclusive.";
                break;
    case QAbstractSocket::SocketAddressNotAvailableError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateFromServerThread The address specified to QAbstractSocket::bind() does not belong to the host.";
                break;
    case QAbstractSocket::UnsupportedSocketOperationError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateFromServerThread The requested socket operation is not supported by the local operating system (e.g., lack of IPv6 support).";
                break;
    case QAbstractSocket::UnknownSocketError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateFromServerThread An unidentified error occurred.";
                break;
    default:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateFromServerThread An unidentified error occurred. 1";
    }
}

void TranslateFromServerThread::initSocket(std::shared_ptr<QTcpSocket> tcpSocket)
{
    this->tcpSocket = tcpSocket;
    mIsinit = true;
}

void TranslateFromServerThread::unblock()
{
    mIsinit = true;
}

void TranslateFromServerThread::setUnblock()
{
    mIsBlockSend = true;
}

void TranslateFromServerThread::run()
{
    ////Event LOOP
    tcpSocket.reset();
    mIsinit = false;
    emit getSocketWithDescriptor(socketDescriptor, true);
    while(!mIsinit && !m_StopThread) {
        msleep(1);
    }

    localPort = tcpSocket->localPort();
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateFromServerThread income in localPort "<<localPort<<" From "<<tcpSocket->peerAddress().toString()<<" IP "<<tcpSocket->peerPort()<< " PORT "<<" - Thread "<<this->currentThreadId();
    connect(tcpSocket.get(), SIGNAL(error(QAbstractSocket::SocketError)),
            this, SLOT(displayError(QAbstractSocket::SocketError)));

    while (!Accessor::getInstance()->isBinded(localPort) && !StopThread()) {
        msleep(100);
    }

    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateFromServerThread was prepared and wait data"<<" - Thread "<<this->currentThreadId();

    std::vector<char> data;
    data.reserve(4096);
    in.setDevice(Accessor::getInstance()->GetSocketFrom(localPort, socketDescriptor).get());

    forever {
        if(StopThread())
            break;

        if(Accessor::getInstance()->GetSocketFrom(localPort, socketDescriptor) != nullptr) {
            if(Accessor::getInstance()->GetSocketFrom(localPort, socketDescriptor)->state() != QTcpSocket::ConnectedState)
                break;

            qint64 size = Accessor::getInstance()->GetSocketFrom(localPort, socketDescriptor)->bytesAvailable();
            if(data.size() < size)
                data.resize(size);

            if(size > 0) {
                int reading = in.readRawData(data.data(), size);

                if(reading == -1) {
                    qCritical()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"SomeBody close Socket";
                    break;
                }

//                Accessor::getInstance()->sendData(localPort, data.data(), reading, socketDescriptor);
                ////Event LOOP
                mIsBlockSend = false;
                emit sendData(localPort, data.data(), reading, socketDescriptor);
                while(!mIsBlockSend && !m_StopThread) {
                    msleep(1);
                }
            }
        }
        else {
            qCritical()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Not Found Socket";
            break;
        }

        msleep(10);
    }

    try {
        if(Accessor::getInstance()->GetSocketFrom(localPort, socketDescriptor) != nullptr)
            Accessor::getInstance()->GetSocketFrom(localPort, socketDescriptor)->disconnectFromHost();
    } catch (...) {
        qCritical()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Try disconnect socket from id - "<<this->currentThreadId();
    }
}
