#include <QtNetwork>
#include <cstdlib>

#include "dialog.h"
#include "LikeAStunServer.h"

static Dialog *pDialog;
class ThreadDeleter;

Dialog::Dialog(QObject *parent)
    : QObject(parent)
{
    pDialog = this;
    signal(SIGINT, &Dialog::exitQt);
    signal(SIGTERM, &(Dialog::exitQt));

    if (!server.listen(QHostAddress::AnyIPv4, 3000)) {
        qCritical()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Threaded TCP Relay Server "<<" Unable to start the server: "<<server.errorString();
        return;
    }

    if (!mStopServer.listen(QHostAddress::AnyIPv4, 2999)) {
        qCritical()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Threaded TCP Relay Server "<<" Unable to start the StopServer: "<<mStopServer.errorString();
        return;
    }

    if (!mPingerServer.listen(QHostAddress::AnyIPv4, 2998)) {
        qCritical()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Threaded TCP Relay Server "<<" Unable to start the StopServer: "<<mPingerServer.errorString();
        return;
    }

    QString ipAddress;
    auto ipAddressesList = QNetworkInterface::allAddresses();
    // use the first non-localhost IPv4 address
    for (const auto & i : ipAddressesList) {
        if (i != QHostAddress::LocalHost &&
            i.toIPv4Address()) {
            ipAddress = i.toString();
            break;
        }
    }
    // if we did not find one, use IPv4 localhost
    if (ipAddress.isEmpty())
        ipAddress = QHostAddress(QHostAddress::LocalHost).toString();

    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Threaded TCP Relay Server Started "<<" MyIP: "<<ipAddress<<" MyPort: "<<server.serverPort();

    Accessor::getInstance();
    threadDeleter = QSharedPointer<ThreadDeleter>(new ThreadDeleter());
    threadDeleter->start();

    QThreadPool::globalInstance()->setMaxThreadCount(10000);
}

Dialog::~Dialog()
{
    try {
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Threaded TCP Relay Destructor";
        server.close();
        mStopServer.close();
        mPingerServer.close();
        threadDeleter->setStopThread(true);
        threadDeleter.reset(); 
        Accessor::getInstance()->stopAllHandle();
        QThreadPool::globalInstance()->waitForDone(10000);
    }
    catch(...) {

    }
}

void Dialog::deleteMe()
{
    try {
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" " << "Shutdown application deleteMe.";
        server.close();
        mStopServer.close();
        mPingerServer.close();
        threadDeleter->setStopThread(true);
        threadDeleter.reset();
        Accessor::getInstance()->stopAllHandle();
        this->deleteLater();
    }
    catch(...) {

    }
}

void Dialog::exitQt(int _)
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" " << "Shutdown application CTRL+C.";
    QCoreApplication::exit(0);
}
