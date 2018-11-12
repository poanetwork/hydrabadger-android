#include "LikeAStunServer.h"
#include "LikeAStunServerThread.h"

#include <cstdlib>
#include <QThreadPool>

//! [0]
LikeAStunServer::LikeAStunServer(QObject *parent)
    : QTcpServer(parent)
{
    listOfThread.clear();
}

LikeAStunServer::~LikeAStunServer()
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Destructor LikeAStunServer";
    foreach(LikeAStunServerThread *thread, listOfThread) {
        deleteFromListThread(thread);
        thread->wait(10);
        thread->deleteLater();
    }
}
//! [0]

//! [1]
void LikeAStunServer::incomingConnection(qintptr socketDescriptor)
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<" LikeAStunServer::incomingConnection";
    auto *thread = new LikeAStunServerThread(socketDescriptor);
    listOfThread.append(thread);

    connect(thread, SIGNAL(finished()), this, SLOT(deleteFromListThread()));
    connect(thread, SIGNAL(error(QAbstractSocket::SocketError)), this, SLOT(deleteFromListThread(QAbstractSocket::SocketError)));
    connect(thread, SIGNAL(initHandle(QString, QString, quint16, quint64)), Accessor::getInstance(), SLOT(initHandle(QString, QString, quint16, quint64)), Qt::BlockingQueuedConnection);

    thread->start();

    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Thread LikeAStunServer start id - "<<thread->currentThreadId();
}

void LikeAStunServer::deleteFromListThread(QAbstractSocket::SocketError)
{
    LikeAStunServerThread *thread;
    QObject* obj=QObject::sender();
    if (auto *tb = qobject_cast<LikeAStunServerThread *>(obj)){
        thread = tb;
        deleteFromListThread(thread);
    }
}

void LikeAStunServer::deleteFromListThread()
{
    LikeAStunServerThread *thread;
    QObject* obj=QObject::sender();
    if (auto *tb = qobject_cast<LikeAStunServerThread *>(obj)){
        thread = tb;
        deleteFromListThread(thread);
    }
}

void LikeAStunServer::deleteFromListThread(LikeAStunServerThread *thread)
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Thread LikeAStunServer deleted id - "<<thread->currentThreadId();
    try {
        if(listOfThread.contains(thread)) {
            listOfThread.removeAt(listOfThread.indexOf(thread));
            thread->setStopThread(true);
            thread->wait(1000);
            thread->deleteLater();
        }
    } catch (...) {
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Erro LikeAStunServerr while deleting thread";
    }
}


//! [1]
