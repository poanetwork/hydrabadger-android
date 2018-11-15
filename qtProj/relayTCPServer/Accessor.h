#ifndef ACCESSOR_H
#define ACCESSOR_H

#include <QObject>
#include <QMutex>
#include <QHash>
#include <QMutexLocker>
#include <QTcpSocket>
#include <QTcpServer>
#include <QSharedPointer>
#include <memory>

class TranslateToServer;
class TranslateFromServer;

#define DIFFDIAPAZONPORT 3000

#define STARTDIAPAZONPORT 3001
#define ENDDIAPAZONPORT 4001

static QMutex MutexForServerWorkers;
using Ports = std::pair<quint16, quint16>;

struct HandleConnect {
    // global param from bind request
    QString UserGlobalUID  = "";
    QString UserGlobalIP   = "";
    quint16 UserGlobalPORT = 0;

    // if after bind connect to port PORTTOSEND
    bool wasBinded = false;

    // server listen only one and translate them
    QSharedPointer<TranslateToServer> serverUserTo;
    // Socket param after Bind connect
    qintptr socketDescriptorBinded;
    std::shared_ptr<QTcpSocket> socketBindedTO;
    // port listened after bind, to establish socketBindedTO
    quint16 PORTTOSEND = 0; // I

    // server listen any and translate
    QSharedPointer<TranslateFromServer> serverUsersFrom;
    // port listen after bind, and all data from PORTFROMLISTEN translate to socketBindedTO
    quint16 PORTFROMLISTEN = 0; // II
    QHash<qintptr, std::shared_ptr<QTcpSocket>> PORTFROMLISTEN_SOCKETSLIST;

    // this for delete unbinded socket
    quint64 secsSinceStartBindRequest;
};


class Accessor : public QObject
{
    // for threadDeleter instanse
    friend class ThreadDeleter;

    Q_OBJECT
public:
    static Accessor *getInstance();
protected:
    Accessor(QObject *parent = nullptr);
    ~Accessor();
    Accessor(const Accessor&);
    Accessor &operator =(const Accessor& );

public slots:
    //API

    // I
    //  - initHandle (QString UserGlobalUID, QString UserGlobalIP, quint16 UserGlobalPORT, quint64 secsSinceStartBindRequest)
    //  Function that get notUsage 2 port and start two servers, and remember HandleConnect
    ///  params:
    // 1. QString UserGlobalUID
    // 2. QString UserGlobalIP
    // 3. quint16 UserGlobalPORT
    // 4. quint64 secsSinceStartBindRequest - for deleting after 3min if wasBinded == false
    /// return:
    // std::pair<quint16, quint16> as Ports in signal
    // first - PORTTOSEND - where you need connect to get relayed data
    // second - PORTFROMLISTEN - port for other, port from gets data
    void initHandle(const QString& UserGlobalUID, const QString& UserGlobalIP, quint16 UserGlobalPORT, quint64 secsSinceStartBindRequest);

    // II
    //  stopHandle (quint16 PORTFROMLISTEN)
    //  Function that stoping all connect from two servers that remembers in HandleConnect, set two ports available for other, delete Handle
    /// return:
    // true - if succesfull
    bool stopHandle(quint16 PORTFROMLISTEN);

    bool stopHandle(quint16 PORTTO, bool);

    bool stopHandle(QSharedPointer<HandleConnect> handle);

    void stopAllHandle();

    void stopHandleWithUID(const QString& UID);

    // III
    //  sendData (quint16 PORTFROMLISTEN, char *data, int len)
    //  Function that send data
    ///  params:
    // 1. quint16 PORTFROMLISTEN
    // 2. char *data
    // 3. qintptr len
    void sendData(quint16 PORTFROMLISTEN, const char *data, int len, qintptr socketdescription);

    // IV
    //  GetSocketFrom(quint16 PORTFROMLISTEN, qintptr socketDescriptor)
    //  Function that get  socket
    ///  params:
    // 1. quint16 PORTFROMLISTEN - where insert
    // 2. qintptr socketDescriptor
    std::shared_ptr<QTcpSocket> GetSocketFrom(quint16 PORTFROMLISTEN, qintptr socketDescriptor);

    // V
    //  GetSocketTo(quint16 PORTTOSEND)
    //  Function that get  socket
    std::shared_ptr<QTcpSocket> GetSocketTo(quint16 PORTTOSEND);

    std::shared_ptr<QTcpSocket> GetSocketTo(quint16 PORTFROMLISTEN, bool);

    // VI
    //  isBinded(quint16 PORTFROMLISTEN)
    //  Function that get  socket
    bool isBinded(quint16 PORTFROMLISTEN);

    // VII
    //  getSocketWithDescriptor(qintptr socketDescriptorBinded, bool fromto)
    //  insert inited socket
    ///  params:
    // 1. qintptr socketDescriptor
    // 2. bool fromto: if true - fromsocket, else tosocket
    void getSocketWithDescriptor(qintptr socketDescriptor, bool fromto);

    // VIII
    // sendDataFreedBack(quint16 PORTTOSend, const char *data, int len, qintptr socketDescriptor);
    //  send data to back
    void sendDataFreedBack(quint16 PORTTOSend, const char *data, int len, qintptr socketDescriptor);

public slots:
    Ports getMeNotUsagePort();
    void  setThisPortNotUsage(Ports pair);

private slots:
    void displayError(QAbstractSocket::SocketError socketError);

    //  Insert_SocketFrom(quint16 PORTFROMLISTEN, qintptr socketDescriptor, QSharedPointer<QTcpSocket> soc)
    //  Function that insert from socket
    ///  params:
    // 1. quint16 PORTFROMLISTEN - where insert
    // 2. qintptr socketDescriptor
    // 3. QSharedPointer<QTcpSocket> soc
    /////
    void Insert_SocketFrom(quint16 PORTFROMLISTEN, qintptr socketDescriptor, std::shared_ptr<QTcpSocket> soc);


    //  setBinded (quint16 PORTTOSEND, qintptr socketDescriptorBinded, std::unique_ptr<QTcpSocket> socketBindedTO)
    //  Function that set Binded value to true
    ///  params:
    // 1. quint16 PORTTOSEND
    // 2. qintptr socketDescriptorBinded
    // 3. std::unique_ptr<QTcpSocket> socketBindedTO
    /// return:
    // true - if succesfull
    // false - if Hnadle Not Exist
    bool setBinded(quint16 PORTTOSEND, qintptr socketDescriptorBinded, std::shared_ptr<QTcpSocket> socketBindedTO);


    quint16 getPORTFROMLISTEN_fromUIDD(const QString& UID);
    quint16 getPORTFROMLISTEN_fromPORTTOSend(quint16 PORTTOSEND);

public:
    quint16 mUsagePortNumber;

protected:
    QHash<quint16, bool> mExistingUsagePort;

private:
    // !For Simply i use fow key PORTFROMLISTEN that all know, value is HandleConnect where was info where send data!
    QHash<quint16, QSharedPointer<HandleConnect>> AllConnectHandles;

    quint16 lastPortStart;
};

#endif // ACCESSOR_H
