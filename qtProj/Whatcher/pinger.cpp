#include "pinger.h"
#include <QNetworkInterface>

pinger::pinger(QObject *parent)
    : QThread(parent)
{
    m_StopThread = false;

    ipAddress = QHostAddress::LocalHost;
    auto ipAddressesList = QNetworkInterface::allAddresses();
    // use the first non-localhost IPv4 address
    for (const auto & i : ipAddressesList) {
        if (i != QHostAddress::LocalHost &&
            i.toIPv4Address()) {
            ipAddress = i;
            break;
        }
    }
}

pinger::~pinger()
{
    setStopThread(true);
    QThread::wait();
}

void pinger::waitForByte(QTcpSocket *socket, int size)
{
    while(socket->bytesAvailable() < size){
        socket->waitForReadyRead();
    }
}

bool pinger::StopThread() const
{
    return m_StopThread;
}

void pinger::error(QAbstractSocket::SocketError socketError)
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"!!ping ERROR";

    emit notPinged();
}

void pinger::run()
{
    msleep(30*1000);

    while (!m_StopThread) {

        tcpSocket.reset();
        tcpSocket = QSharedPointer<QTcpSocket>(new QTcpSocket());
        tcpSocket->connectToHost(ipAddress.toString(), 2998);
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"try connect to "<<ipAddress.toString()<<" "<<2998;
        tcpSocket->waitForConnected();
        connect(tcpSocket.data(), SIGNAL(error(QAbstractSocket::SocketError)), this, SLOT(error(QAbstractSocket::SocketError)));
        in.setDevice(tcpSocket.data());

        if(tcpSocket->state() != QAbstractSocket::ConnectedState) {
            emit notPinged();
            return;
        }

        QByteArray block;
        QDataStream out(&block, QIODevice::WriteOnly);
        out << 0xCAFECAFE;
        tcpSocket->write(block);
        tcpSocket->flush();
        tcpSocket->waitForBytesWritten();

        waitForByte(tcpSocket.data(), sizeof(QString("OK")));

        in.startTransaction();
        QString nextFortune;
        in >> nextFortune;

        if(nextFortune != "OK") {
            qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"ping error";

            emit notPinged();
            return;
        }

        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"ping success";

        msleep(60*1000);
    }
}


void pinger::setStopThread(bool StopThread)
{
    m_StopThread = StopThread;
}
