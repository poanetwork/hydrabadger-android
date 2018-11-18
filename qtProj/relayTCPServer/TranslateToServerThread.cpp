#include "TranslateToServerThread.h"

#include <utility>
#include <QDateTime>

TranslateToServerThread::TranslateToServerThread(qintptr socketDescriptor, QObject *parent)
    : QThread(parent), socketDescriptor(socketDescriptor), m_StopThread(false)
{
}

TranslateToServerThread::~TranslateToServerThread()
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<"Destructor TranslateToServerThread";
    setStopThread(true);
    QThread::wait();
}

bool TranslateToServerThread::StopThread() const
{
    return m_StopThread;
}


void TranslateToServerThread::setStopThread(bool StopThread)
{
    if (m_StopThread == StopThread)
        return;

    m_StopThread = StopThread;
    emit StopThreadChanged(m_StopThread);
}

void TranslateToServerThread::waitForByte(QTcpSocket *socket, int size)
{
    while(!m_StopThread && socket->state() == QTcpSocket::ConnectedState && socket->bytesAvailable() < size){
        socket->waitForReadyRead(1000);
        sleep(1);
    }
}

void TranslateToServerThread::displayError(QAbstractSocket::SocketError socketError)
{
    switch (socketError) {
    case QAbstractSocket::ConnectionRefusedError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateToServerThread The connection was refused by the peer (or timed out).";
                break;
    case QAbstractSocket::RemoteHostClosedError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateToServerThread The remote host closed the connection. Note that the client socket (i.e., this socket) will be closed after the remote close notification has been sent.";
                break;
    case QAbstractSocket::HostNotFoundError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateToServerThread The host address was not found.";
                break;
    case QAbstractSocket::SocketAccessError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateToServerThread The socket operation failed because the application lacked the required privileges.";
                break;
    case QAbstractSocket::SocketResourceError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateToServerThread The local system ran out of resources (e.g., too many sockets).";
                break;
    case QAbstractSocket::SocketTimeoutError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateToServerThread The socket operation timed out.";
                break;
    case QAbstractSocket::DatagramTooLargeError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateToServerThread The datagram was larger than the operating system's limit (which can be as low as 8192 bytes).";
                break;
    case QAbstractSocket::NetworkError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateToServerThread An error occurred with the network (e.g., the network cable was accidentally plugged out).";
                break;
    case QAbstractSocket::AddressInUseError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateToServerThread The address specified to QAbstractSocket::bind() is already in use and was set to be exclusive.";
                break;
    case QAbstractSocket::SocketAddressNotAvailableError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateToServerThread The address specified to QAbstractSocket::bind() does not belong to the host.";
                break;
    case QAbstractSocket::UnsupportedSocketOperationError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateToServerThread The requested socket operation is not supported by the local operating system (e.g., lack of IPv6 support).";
                break;
    case QAbstractSocket::UnknownSocketError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateToServerThread An unidentified error occurred.";
                break;
    default:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateToServerThread An unidentified error occurred. 1";
    }
}

void TranslateToServerThread::initSocket(std::shared_ptr<QTcpSocket> tcpSocket)
{
    this->tcpSocket = tcpSocket;
    mIsinit = true;
}

void TranslateToServerThread::unblock()
{
    mIsinit = true;
}

void TranslateToServerThread::setUnblock()
{
    mIsBlockSend = true;
}

void TranslateToServerThread::disconnect()
{
    if(tcpSocket.get() != NULL) {
        tcpSocket->disconnectFromHost();
    }
}

void TranslateToServerThread::run()
{
    ////Event LOOP
    tcpSocket.reset();
    mIsinit = false;
    emit getSocketWithDescriptor(socketDescriptor, false);
    while(!mIsinit && !m_StopThread) {
        msleep(1);
    }

    quint16 localPort = tcpSocket->localPort();
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Was binded on port "<<localPort<<" From "<<tcpSocket->peerAddress().toString()<<" IP "<<tcpSocket->peerPort()<< " PORT "<<" - theadId "<<this->currentThreadId();

    connect(tcpSocket.get(), SIGNAL(error(QAbstractSocket::SocketError)),
            this, SLOT(displayError(QAbstractSocket::SocketError)));

    in.setDevice(Accessor::getInstance()->GetSocketTo(localPort).get());
    QByteArray data;
    data.reserve(4096);
    forever {
        if(StopThread())
            break;

        if(Accessor::getInstance()->GetSocketTo(localPort).get() != nullptr) {
            if(Accessor::getInstance()->GetSocketTo(localPort)->state() != QTcpSocket::ConnectedState) {
                emit stopHandle(localPort, true);
                break;
            }

            qint64 size = Accessor::getInstance()->GetSocketTo(localPort)->bytesAvailable();
            if(size > 0) {
//                QMutexLocker locker(&MutexForServerWorkers);
                waitForByte(Accessor::getInstance()->GetSocketTo(localPort).get(), 2*sizeof(qint32));

                qint32 size1 = 0;
                in >> size1;

                qint32 socketdescriptor = 0;
                in >> socketdescriptor;

                waitForByte(Accessor::getInstance()->GetSocketTo(localPort).get(), size1);

                data.clear();
                if(data.size() < size1)
                    data.resize(size1);

                int bytesRead = 0;
                while (bytesRead < size1) {
                    int res = in.readRawData(&data.data()[bytesRead], size1);
                    if(res == -1)
                        break;
                    bytesRead += res;
                }
//                locker.unlock();

                quint16 portfromlisten = Accessor::getInstance()->isValidSocketFreedBack(localPort, socketdescriptor);
                if(portfromlisten != 0) {
                    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"sendDataFreedBack "<<size1<<" bytes from portTO "<<localPort<<" for portfrom "<<portfromlisten<<" sockdescr "<<socketdescriptor;
                    ////Event LOOP
                    mIsBlockSend = false;
                    emit sendDataFreedBack(portfromlisten, data.data(), size1, socketdescriptor);
                    while(!mIsBlockSend && !m_StopThread) {
                        msleep(1);
                    }
                }
                else {
                    qCritical()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"sendDataFreedBack ERROR from portTO "<<localPort<<" for portfrom "<<portfromlisten<<" DROP "<<size1<<" bytes";
                }
            }
        }
        else {
            emit stopHandle(localPort, true);
            break;
        }

        msleep(10);
    }

    try {
        if(Accessor::getInstance()->GetSocketTo(localPort) != nullptr)
            Accessor::getInstance()->GetSocketTo(localPort)->disconnectFromHost();
    } catch (...) {
        qCritical()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"SomeBody Close socketBindedTO ";
    }
}
