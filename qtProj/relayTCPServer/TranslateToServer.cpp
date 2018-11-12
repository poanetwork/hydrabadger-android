#include "TranslateToServer.h"
#include <QDateTime>

TranslateToServer::TranslateToServer(QObject *parent)
    : QTcpServer(parent)
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateToServer create";
    listOfThread.clear();
}

TranslateToServer::~TranslateToServer()
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Destructor TranslateToServer";
    foreach(TranslateToServerThread *thread, listOfThread) {
        thread->setStopThread(true);
        deleteFromListThread(thread);
        thread->wait(10);
        thread->deleteLater();
    }
}

void TranslateToServer::incomingConnection(qintptr socketDescriptor)
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<" TranslateToServer::incomingConnection";
    auto *thread = new TranslateToServerThread(socketDescriptor);
    listOfThread.append(thread);

    connect(thread, SIGNAL(finished()), this, SLOT(deleteFromListThread()));
//    connect(thread, SIGNAL(error(QAbstractSocket::SocketError)), this, SLOT(deleteFromListThread(QAbstractSocket::SocketError)));

    connect(thread, SIGNAL(getSocketWithDescriptor(qintptr, bool)), Accessor::getInstance(), SLOT(getSocketWithDescriptor(qintptr, bool)), Qt::BlockingQueuedConnection);
    connect(thread, SIGNAL(sendDataFreedBack(quint16, const char *, int, int)), Accessor::getInstance(), SLOT(sendDataFreedBack(quint16, const char *, int, int)), Qt::BlockingQueuedConnection);
    connect(thread, SIGNAL(stopHandle(quint16,bool)), Accessor::getInstance(), SLOT(stopHandle(quint16, bool)), Qt::BlockingQueuedConnection);

    thread->start();

    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Thread TranslateToServer start id - "<<thread->currentThreadId();
}

void TranslateToServer::deleteFromListThread()
{
    TranslateToServerThread *thread;
    QObject* obj=QObject::sender();
    if (auto *tb = qobject_cast<TranslateToServerThread *>(obj)){
        thread = tb;
        deleteFromListThread(thread);
    }
}

void TranslateToServer::deleteFromListThread(QAbstractSocket::SocketError)
{
    TranslateToServerThread *thread;
    QObject* obj=QObject::sender();
    if (auto *tb = qobject_cast<TranslateToServerThread *>(obj)){
        thread = tb;
        deleteFromListThread(thread);
    }
}

void TranslateToServer::deleteFromListThread(TranslateToServerThread *thread)
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Thread TranslateToServer deleted id - "<<thread->currentThreadId();
    try {
        if(listOfThread.contains(thread)) {
            listOfThread.removeAt(listOfThread.indexOf(thread));
            thread->setStopThread(true);
            thread->wait(10000);
            thread->deleteLater();
        }
    } catch (...) {
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Error TranslateToServer while deleting thread";
    }
}
