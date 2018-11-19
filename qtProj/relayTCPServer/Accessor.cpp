#include "Accessor.h"
#include <QMutexLocker>
#include <QDebug>
#include <QElapsedTimer>
#include "ThreadDeleter.h"
#include "TranslateFromServer.h"
#include "TranslateToServer.h"
#include <memory>

Accessor *Accessor::getInstance()
{
    static Accessor instance;
    return &instance;
}

Accessor::Accessor(QObject *parent) : QObject(parent)
{
    mUsagePortNumber = 0;
    lastPortStart    = 0;
}

Accessor::~Accessor()
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Destructor Accessor";
    stopAllHandle();
}

void Accessor::initHandle(const QString& UserGlobalUID, const QString& UserGlobalIP, quint16 UserGlobalPORT, quint64 secsSinceStartBindRequest)
{
    QElapsedTimer timer;
    timer.restart();
    QSharedPointer<HandleConnect> handle = QSharedPointer<HandleConnect>(new HandleConnect());

    handle->UserGlobalUID  = UserGlobalUID;
    handle->UserGlobalIP   = UserGlobalIP;
    handle->UserGlobalPORT = UserGlobalPORT;
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"initHandle - UserGlobalUID: "<<UserGlobalUID<<" UserGlobalIP: "<<UserGlobalIP<<" UserGlobalPORT: "<<UserGlobalPORT;
    // add 5sec
    handle->secsSinceStartBindRequest = secsSinceStartBindRequest+5;

    // get 2 ports not in use
    Ports ports = getMeNotUsagePort();
    handle->PORTTOSEND     = ports.first;
    handle->PORTFROMLISTEN = ports.second;

    MutexForServerWorkers.lock();
    AllConnectHandles.insert(ports.second, handle);
    MutexForServerWorkers.unlock();

    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"initHandle - UserGlobalUID: "<<UserGlobalUID<<" set two ports: for user - "<<AllConnectHandles[ports.second]->PORTTOSEND<<" for other "<<AllConnectHandles[ports.second]->PORTFROMLISTEN;

    // start 2 servers
    AllConnectHandles[ports.second]->serverUserTo = QSharedPointer<TranslateToServer>(new TranslateToServer());
    if (!AllConnectHandles[ports.second]->serverUserTo->listen(QHostAddress::AnyIPv4, AllConnectHandles[ports.second]->PORTTOSEND)) {
        qCritical()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Threaded Server TranslateToServer "<<"Unable to start the server: "<<AllConnectHandles[ports.second]->serverUserTo->errorString();
        stopHandle(AllConnectHandles[ports.second]);
        QMetaObject::invokeMethod(sender(), "beenInitedHandle", Qt::DirectConnection, Q_ARG(Ports, Ports(-1,-1)));
    }

    AllConnectHandles[ports.second]->serverUsersFrom = QSharedPointer<TranslateFromServer>(new TranslateFromServer());
    if (!AllConnectHandles[ports.second]->serverUsersFrom->listen(QHostAddress::AnyIPv4, AllConnectHandles[ports.second]->PORTFROMLISTEN)) {
        qCritical()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Threaded Server TranslateFromServer "<<"Unable to start the server: "<<AllConnectHandles[ports.second]->serverUsersFrom->errorString();
        stopHandle(AllConnectHandles[ports.second]);
        QMetaObject::invokeMethod(sender(), "beenInitedHandle", Qt::DirectConnection, Q_ARG(Ports, Ports(-1,-1)));
    }
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"initHandle - started two servers";

    AllConnectHandles[ports.second]->wasBinded = false;

    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"initHandle - Compited at "<<timer.elapsed()<<" ms";

    QMetaObject::invokeMethod(sender(), "beenInitedHandle", Qt::DirectConnection, Q_ARG(Ports, ports));
}

bool Accessor::stopHandle(QSharedPointer<HandleConnect> handle)
{
    if(handle != nullptr && handle->wasBinded) {
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"stopHandle - this is binded handle";

        try {
            if(handle->socketBindedTO != nullptr) {
                qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"stopHandle - socketBindedTO close";
                handle->socketBindedTO->disconnectFromHost();
                handle->socketBindedTO->close();
                handle->socketBindedTO.reset();
            }
        } catch (...) {
            qCritical()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"ERR while socketBindedTO deleteLater";
        }

        foreach (auto key, handle->PORTFROMLISTEN_SOCKETSLIST.keys()) {
            try {
                qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"stopHandle - try close sockets on socketDiscription - "<<key;
                std::shared_ptr<QTcpSocket> soc = handle->PORTFROMLISTEN_SOCKETSLIST.take(key);
                if(soc != nullptr)  {
                    soc->disconnectFromHost();
                    soc->close();
                    soc.reset();
                }
            } catch (...) {
                qCritical()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"ERR while PORTFROMLISTEN_SOCKETSLIST deleteLater";
            }
        }
    }
    else {
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"stopHandle - not binded";
    }

    setThisPortNotUsage(Ports(handle->PORTTOSEND, handle->PORTFROMLISTEN));

    //close Servers
    try {
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"stopHandle - try close serverUserTo";
        if(handle->serverUserTo.get() != nullptr) {
            handle->serverUserTo->close();
            handle->serverUserTo.reset();
        }
    } catch (...) {
        qCritical()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"ERR while serverUserTo close";
    }

    try {
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"stopHandle - try close serverUsersFrom";
        if(handle->serverUsersFrom.get() != nullptr) {
            handle->serverUsersFrom->close();
            handle->serverUsersFrom.reset();
        }
    } catch (...) {
        qCritical()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"ERR while serverUsersFrom close";
    }

    handle.reset();

    return true;
}

bool Accessor::stopHandle(quint16 PORTTOSEND, bool)
{
    quint16 PORTFROMLISTEN = getPORTFROMLISTEN_fromPORTTOSend(PORTTOSEND);
    if(PORTFROMLISTEN == 0) {
        qCritical()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"stopHandle not found associated PORTFROMLISTEN";
        return false;
    }

    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"stopHandle - not inited handle PORTFROMLISTEN - "<<PORTFROMLISTEN;

    if(AllConnectHandles.contains(PORTFROMLISTEN)) {
        QSharedPointer<HandleConnect> handle = AllConnectHandles.take(PORTFROMLISTEN);

        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" Accessor stopHandle PORTFROMLISTEN "<< PORTFROMLISTEN;

        stopHandle(handle);
        return true;
    }

    return false;
}

bool Accessor::stopHandle(quint16 PORTFROMLISTEN)
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"stopHandle - not inited handle PORTFROMLISTEN - "<<PORTFROMLISTEN;

    if(AllConnectHandles.contains(PORTFROMLISTEN)) {
        QSharedPointer<HandleConnect> handle = AllConnectHandles.take(PORTFROMLISTEN);

        stopHandle(handle);

        return true;
    }

    return false;
}

bool Accessor::setBinded(quint16 PORTTOSEND, qintptr socketDescriptorBinded, std::shared_ptr<QTcpSocket> socketBindedTO)
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"setBinded - binded socket PORTTOSEND: "<< PORTTOSEND;

    quint16 PORTFROMLISTEN = getPORTFROMLISTEN_fromPORTTOSend(PORTTOSEND);
    if(PORTFROMLISTEN == 0) {
        qCritical()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"setBinded not found associated PORTFROMLISTEN";
        return false;
    }

    if (AllConnectHandles.contains(PORTFROMLISTEN)) {
        AllConnectHandles.value(PORTFROMLISTEN)->wasBinded = true;
        AllConnectHandles.value(PORTFROMLISTEN)->socketDescriptorBinded = socketDescriptorBinded;
        AllConnectHandles.value(PORTFROMLISTEN)->socketBindedTO = socketBindedTO;
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"setBinded - wait data on PORTTOSEND: "<< AllConnectHandles.value(PORTFROMLISTEN)->socketBindedTO->localPort();
        return true;
    }

    return false;
}

quint16 Accessor::getPORTFROMLISTEN_fromUIDD(const QString &UID)
{
    quint16 PORTFROMLISTEN = 0;
    foreach (quint16 key, AllConnectHandles.keys()) {
        if(AllConnectHandles.value(key)->UserGlobalUID == UID) {
            PORTFROMLISTEN = AllConnectHandles.value(key)->PORTFROMLISTEN;
            break;
        }
    }
    return PORTFROMLISTEN;
}

quint16 Accessor::getPORTFROMLISTEN_fromPORTTOSend(quint16 PORTTOSEND)
{
    quint16 PORTFROMLISTEN = 0;
    foreach (quint16 key, AllConnectHandles.keys()) {
        if(AllConnectHandles.value(key)->PORTTOSEND == PORTTOSEND) {
            PORTFROMLISTEN = AllConnectHandles.value(key)->PORTFROMLISTEN;
            break;
        }
    }
    return PORTFROMLISTEN;
}

void Accessor::stopAllHandle()
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"stopAllHandle";
    foreach (quint16 key, AllConnectHandles.keys()) {
        stopHandle(key);
    }
}



void Accessor::Insert_SocketFrom(quint16 PORTFROMLISTEN, qintptr socketDescriptor, std::shared_ptr<QTcpSocket> soc)
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Insert_SocketFrom - PORTFROMLISTEN: "<<PORTFROMLISTEN;
    if(AllConnectHandles.contains(PORTFROMLISTEN)) {
        AllConnectHandles.value(PORTFROMLISTEN)->PORTFROMLISTEN_SOCKETSLIST.insert(socketDescriptor, soc);
    }
}

std::shared_ptr<QTcpSocket> Accessor::GetSocketFrom(quint16 PORTFROMLISTEN, qintptr socketDescriptor)
{
    if(AllConnectHandles.contains(PORTFROMLISTEN) && AllConnectHandles.value(PORTFROMLISTEN)->PORTFROMLISTEN_SOCKETSLIST.value(socketDescriptor) != nullptr) {
        return AllConnectHandles.value(PORTFROMLISTEN)->PORTFROMLISTEN_SOCKETSLIST.value(socketDescriptor);
    }

    return nullptr;
}

std::shared_ptr<QTcpSocket> Accessor::GetSocketTo(quint16 PORTTOSEND)
{
    quint16 PORTFROMLISTEN = getPORTFROMLISTEN_fromPORTTOSend(PORTTOSEND);
    if(PORTFROMLISTEN == 0)
        return nullptr;

    if(AllConnectHandles.contains(PORTFROMLISTEN) && AllConnectHandles.value(PORTFROMLISTEN)->socketBindedTO.get() != nullptr) {
        return AllConnectHandles.value(PORTFROMLISTEN)->socketBindedTO;
    }

    return nullptr;
}

std::shared_ptr<QTcpSocket> Accessor::GetSocketTo(quint16 PORTFROMLISTEN, bool)
{
    if(AllConnectHandles.contains(PORTFROMLISTEN) && AllConnectHandles.value(PORTFROMLISTEN)->socketBindedTO.get() != nullptr) {
        return AllConnectHandles.value(PORTFROMLISTEN)->socketBindedTO;
    }

    return nullptr;
}

bool Accessor::isBinded(quint16 PORTFROMLISTEN)
{
    if(AllConnectHandles.contains(PORTFROMLISTEN)) {
        return AllConnectHandles.value(PORTFROMLISTEN)->wasBinded;
    }
    return false;
}

void Accessor::stopHandleWithUID(const QString& UID)
{
    quint16 PORTFROMLISTEN = getPORTFROMLISTEN_fromUIDD(UID);
    if(PORTFROMLISTEN == 0)
        QMetaObject::invokeMethod(sender(), "setUnblock", Qt::DirectConnection);

    if(AllConnectHandles.contains(PORTFROMLISTEN)) {
        stopHandle(PORTFROMLISTEN);

        QMetaObject::invokeMethod(sender(), "setUnblock", Qt::DirectConnection);
    }
}

void Accessor::displayError(QAbstractSocket::SocketError socketError)
{
    switch (socketError) {
    case QAbstractSocket::ConnectionRefusedError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Accessor The connection was refused by the peer (or timed out).";
        break;
    case QAbstractSocket::RemoteHostClosedError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Accessor The remote host closed the connection. Note that the client socket (i.e., this socket) will be closed after the remote close notification has been sent.";
        break;
    case QAbstractSocket::HostNotFoundError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Accessor The host address was not found.";
        break;
    case QAbstractSocket::SocketAccessError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Accessor The socket operation failed because the application lacked the required privileges.";
        break;
    case QAbstractSocket::SocketResourceError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Accessor The local system ran out of resources (e.g., too many sockets).";
        break;
    case QAbstractSocket::SocketTimeoutError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Accessor The socket operation timed out.";
        break;
    case QAbstractSocket::DatagramTooLargeError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Accessor The datagram was larger than the operating system's limit (which can be as low as 8192 bytes).";
        break;
    case QAbstractSocket::NetworkError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Accessor An error occurred with the network (e.g., the network cable was accidentally plugged out).";
        break;
    case QAbstractSocket::AddressInUseError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Accessor The address specified to QAbstractSocket::bind() is already in use and was set to be exclusive.";
        break;
    case QAbstractSocket::SocketAddressNotAvailableError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Accessor The address specified to QAbstractSocket::bind() does not belong to the host.";
        break;
    case QAbstractSocket::UnsupportedSocketOperationError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Accessor The requested socket operation is not supported by the local operating system (e.g., lack of IPv6 support).";
        break;
    case QAbstractSocket::UnknownSocketError:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Accessor An unidentified error occurred.";
        break;
    default:
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Accessor An unidentified error occurred. 1";
    }
}

void Accessor::getSocketWithDescriptor(qintptr socketDescriptor, bool fromto)
{
    auto tcpSocket = std::shared_ptr<QTcpSocket>(new QTcpSocket());

    if (!tcpSocket->setSocketDescriptor(socketDescriptor)) {
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Error QTcpSocket listened not start";
        QMetaObject::invokeMethod(sender(), "unblock", Qt::DirectConnection);
    }

    quint16 localPort = tcpSocket->localPort();
    if(fromto) {
        Insert_SocketFrom(localPort, socketDescriptor, tcpSocket);

        QMetaObject::invokeMethod(sender(), "initSocket", Qt::DirectConnection, Q_ARG(std::shared_ptr<QTcpSocket>, GetSocketFrom(localPort, socketDescriptor)));
    }
    else {
        setBinded(localPort, socketDescriptor, tcpSocket);

        QMetaObject::invokeMethod(sender(), "initSocket", Qt::DirectConnection, Q_ARG(std::shared_ptr<QTcpSocket>, GetSocketTo(localPort)));
    }
}

void Accessor::sendData(quint16 PORTFROMLISTEN, QByteArray block, const char *data, int len)
{
//    QMutexLocker locker(&MutexForServerWorkers);
    AllConnectHandles.value(PORTFROMLISTEN)->socketBindedTO->write(block);
    AllConnectHandles.value(PORTFROMLISTEN)->socketBindedTO->write(data, len);
    AllConnectHandles.value(PORTFROMLISTEN)->socketBindedTO->flush();
    AllConnectHandles.value(PORTFROMLISTEN)->socketBindedTO->waitForBytesWritten();
    QMetaObject::invokeMethod(sender(), "setUnblock", Qt::DirectConnection);
}

bool Accessor::isValidSocketsendData(quint16 PORTFROMLISTEN)
{
    if(AllConnectHandles.contains(PORTFROMLISTEN)
            && AllConnectHandles.value(PORTFROMLISTEN)->socketBindedTO.get() != nullptr
            )
    {
        return true;
    }

    return false;
}


void Accessor::sendDataFreedBack(quint16 PORTFROMLISTEN, const char *data, int len, qintptr socketDescriptor)
{
//    QMutexLocker locker(&MutexForServerWorkers);
    AllConnectHandles.value(PORTFROMLISTEN)->PORTFROMLISTEN_SOCKETSLIST.value(socketDescriptor)->write(data, len);
    AllConnectHandles.value(PORTFROMLISTEN)->PORTFROMLISTEN_SOCKETSLIST.value(socketDescriptor)->flush();
    AllConnectHandles.value(PORTFROMLISTEN)->PORTFROMLISTEN_SOCKETSLIST.value(socketDescriptor)->waitForBytesWritten();
}


quint16 Accessor::isValidSocketFreedBack(quint16 PORTTOSEND, qintptr socketDescriptor)
{
    quint16 PORTFROMLISTEN = getPORTFROMLISTEN_fromPORTTOSend(PORTTOSEND);

    if(AllConnectHandles.contains(PORTFROMLISTEN)
            && AllConnectHandles.value(PORTFROMLISTEN)->PORTFROMLISTEN_SOCKETSLIST.contains(socketDescriptor)
            && AllConnectHandles.value(PORTFROMLISTEN)->PORTFROMLISTEN_SOCKETSLIST.value(socketDescriptor).get() != nullptr
            )
    {
        return PORTFROMLISTEN;
    }

    return 0;
}


Ports Accessor::getMeNotUsagePort()
{
    QMutexLocker locker(&MutexForServerWorkers);

    int diapazon = ENDDIAPAZONPORT - STARTDIAPAZONPORT;

    for(quint16 i = 1; i < diapazon; i+=2) {
        quint16 portStart = (lastPortStart + i) % diapazon;

        if(!mExistingUsagePort.contains(portStart) && !mExistingUsagePort.contains(portStart+1)) {
            mExistingUsagePort[portStart  ] = true;
            mExistingUsagePort[portStart+1] = true;
            mUsagePortNumber++;
            mUsagePortNumber++;

            lastPortStart = (portStart+1)%diapazon;

            qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"setThisPortUsed "<<portStart+DIFFDIAPAZONPORT<<" "<<portStart+1+DIFFDIAPAZONPORT;

            return Ports(portStart+DIFFDIAPAZONPORT, portStart+1+DIFFDIAPAZONPORT);
        }
        else if(mExistingUsagePort.contains(portStart) && mExistingUsagePort.contains(portStart+1)) {
            if(!mExistingUsagePort[portStart ] && !mExistingUsagePort[portStart+1]) {
                mExistingUsagePort[portStart  ] = true;
                mExistingUsagePort[portStart+1] = true;
                mUsagePortNumber++;
                mUsagePortNumber++;

                lastPortStart = (portStart+1)%diapazon;

                qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"setThisPortUsed "<<portStart+DIFFDIAPAZONPORT<<" "<<portStart+DIFFDIAPAZONPORT+1;

                return Ports(portStart+DIFFDIAPAZONPORT, portStart+DIFFDIAPAZONPORT+1);
            }
        }

    }

    return Ports(-1, -1);
}

void Accessor::setThisPortNotUsage(Ports pair)
{
    QMutexLocker locker(&MutexForServerWorkers);

    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"setThisPortNotUsage "<<pair.first<<" "<<pair.second;

    mExistingUsagePort[pair.first -DIFFDIAPAZONPORT] = false;
    mExistingUsagePort[pair.second-DIFFDIAPAZONPORT] = false;
    mUsagePortNumber--;
    mUsagePortNumber--;

}

