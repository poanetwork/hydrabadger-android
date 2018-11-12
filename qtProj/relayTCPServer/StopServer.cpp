#include "StopServer.h"
#include <QDateTime>

StopServer::StopServer(QObject *parent) : QTcpServer(parent)
{
    listOfThread.clear();
}

StopServer::~StopServer()
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Destructor StopServer";
    foreach(StopServerThread *thread, listOfThread) {
        deleteFromListThread(thread);
        thread->wait(10);
        thread->deleteLater();
    }
}

void StopServer::incomingConnection(qintptr socketDescriptor)
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<" StopServer::incomingConnection";
    auto *thread = new StopServerThread(socketDescriptor);
    listOfThread.append(thread);

    connect(thread, SIGNAL(stopHandleWithUID(QString)), Accessor::getInstance(), SLOT(stopHandleWithUID(QString)), Qt::BlockingQueuedConnection);
    connect(thread, SIGNAL(finished()), this, SLOT(deleteFromListThread()));
    connect(thread, SIGNAL(error(QAbstractSocket::SocketError)), this, SLOT(deleteFromListThread(QAbstractSocket::SocketError)));

    thread->start();

    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Thread StopServer start id - "<<thread->currentThreadId();
}

void StopServer::deleteFromListThread()
{
    StopServerThread *thread;
    QObject* obj=QObject::sender();
    if (auto *tb = qobject_cast<StopServerThread *>(obj)){
        thread = tb;
        deleteFromListThread(thread);
    }
}

void StopServer::deleteFromListThread(QAbstractSocket::SocketError)
{
    StopServerThread *thread;
    QObject* obj=QObject::sender();
    if (auto *tb = qobject_cast<StopServerThread *>(obj)){
        thread = tb;
        deleteFromListThread(thread);
    }
}

void StopServer::deleteFromListThread(StopServerThread *thread)
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Thread StopServer deleted id - "<<thread->currentThreadId();
    try {
        if(listOfThread.contains(thread)) {
            listOfThread.removeAt(listOfThread.indexOf(thread));
            thread->setStopThread(true);
            thread->wait(1000);
            thread->deleteLater();
        }
    } catch (...) {
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Error while StopServer deleting thread";
    }
}
