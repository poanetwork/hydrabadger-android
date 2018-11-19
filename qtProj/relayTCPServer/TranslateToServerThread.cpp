#include "TranslateToServerThread.h"

#include <utility>
#include <QDateTime>

TranslateToServerThread::TranslateToServerThread(qintptr socketDescriptor, QObject *parent)
    : QThread(parent), m_StopThread(false), socketDescriptor(socketDescriptor)
{
    data.reserve(4096);
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
//    case QAbstractSocket::SocketTimeoutError:
//        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateToServerThread The socket operation timed out.";
//                break;
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
        ;
//        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateToServerThread An unidentified error occurred. 1";
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

void TranslateToServerThread::disconnect()
{
    setStopThread(true);
    if(tcpSocket.get() != NULL) {
        tcpSocket->disconnectFromHost();
        if(localPort != 0)
            emit stopHandle(localPort, true);
    }
}

void TranslateToServerThread::onReadyRead()
{
    QDataStream in(tcpSocket.get());
    //if we read the first block, the first qint32 bytes are its size
    if (_blockSize == 0) {
        //if it is less than qint32 bytes, wait until it is qint32 bytes
        if (tcpSocket->bytesAvailable() < (int)sizeof(qint32))
            return;
        //read size (qint32 bytes)
        in >> _blockSize;

        _blockSizeLast = _blockSize;
    }
    //we wait until the block comes completely
    if (tcpSocket->bytesAvailable() < _blockSize)
        return;
    else
        //you can take a new block
        _blockSize = 0;

    qint32 socketdescriptor = 0;
    in >> socketdescriptor;

    if(data.size() < _blockSizeLast)
        data.resize(_blockSizeLast);

    int bytesRead = 0;
    while (bytesRead < _blockSizeLast) {
        int res = in.readRawData(&data.data()[bytesRead], _blockSizeLast-bytesRead);
        if(res == -1)
            break;
        bytesRead += res;
    }

    quint16 portfromlisten = Accessor::getInstance()->isValidSocketFreedBack(localPort, socketdescriptor);
    if(portfromlisten != 0) {
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"sendDataFreedBack "<<_blockSizeLast<<" bytes from portTO "<<localPort<<" for portfrom "<<portfromlisten<<" sockdescr "<<socketdescriptor;
        Accessor::getInstance()->sendDataFreedBack(portfromlisten, data.data(), _blockSizeLast, socketdescriptor);
    }
    else {
        qCritical()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"sendDataFreedBack ERROR from portTO "<<localPort<<" for portfrom "<<portfromlisten<<" DROP "<<_blockSizeLast<<" bytes";
    }
}

void TranslateToServerThread::run()
{
    ////Event LOOP
    tcpSocket.reset();
    mIsinit = false;
    emit getSocketWithDescriptor(socketDescriptor, false);
    while(!mIsinit && !m_StopThread) {
        msleep(100);
    }

    localPort = tcpSocket->localPort();
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Was binded on port "<<localPort<<" From "<<tcpSocket->peerAddress().toString()<<" IP "<<tcpSocket->peerPort()<< " PORT "<<" - theadId "<<this->currentThreadId();

    connect(tcpSocket.get(), SIGNAL(error(QAbstractSocket::SocketError)),
            this, SLOT(displayError(QAbstractSocket::SocketError)));
    connect(tcpSocket.get(), SIGNAL(disconnected()), this, SLOT(disconnect()));
    connect(tcpSocket.get(), SIGNAL(readyRead()), this, SLOT(onReadyRead()));

    forever {
        if(StopThread())
            break;

        msleep(100);
    }

    try {
        if(Accessor::getInstance()->GetSocketTo(localPort) != nullptr)
            Accessor::getInstance()->GetSocketTo(localPort)->disconnectFromHost();
    } catch (...) {
        qCritical()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"SomeBody Close socketBindedTO ";
    }
}
