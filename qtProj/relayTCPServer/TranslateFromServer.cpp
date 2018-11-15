#include "TranslateFromServer.h"
#include <QDateTime>

TranslateFromServer::TranslateFromServer(QObject *parent)
    : QTcpServer(parent)
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"TranslateFromServer create";
    listOfThread.clear();
}

TranslateFromServer::~TranslateFromServer()
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Destructor TranslateFromServer";
    foreach(TranslateFromServerThread *thread, listOfThread) {
        deleteFromListThread(thread);
        thread->wait(10);
        thread->deleteLater();
    }
}

void TranslateFromServer::incomingConnection(qintptr socketDescriptor)
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<" TranslateFromServer::incomingConnection";
    auto *thread = new TranslateFromServerThread(socketDescriptor);
    listOfThread.append(thread);

    connect(thread, SIGNAL(finished()), this, SLOT(deleteFromListThread()));
    connect(thread, SIGNAL(getSocketWithDescriptor(qintptr, bool)), Accessor::getInstance(), SLOT(getSocketWithDescriptor(qintptr, bool)), Qt::BlockingQueuedConnection);
    connect(thread, SIGNAL(sendData(quint16, const char *, int, qintptr)), Accessor::getInstance(), SLOT(sendData(quint16, const char *, int, qintptr)), Qt::BlockingQueuedConnection);

    thread->start();

    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Thread TranslateFromServer start id - "<<thread->currentThreadId();
}

void TranslateFromServer::deleteFromListThread()
{
    TranslateFromServerThread *thread;
    QObject* obj=QObject::sender();
    if (auto *tb = qobject_cast<TranslateFromServerThread *>(obj)){
        thread = tb;
        deleteFromListThread(thread);
    }
}

void TranslateFromServer::deleteFromListThread(TranslateFromServerThread *thread)
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Thread  TranslateFromServer deleted id - "<<thread->currentThreadId();
    try {
        if(listOfThread.contains(thread)) {
            listOfThread.removeAt(listOfThread.indexOf(thread));
            thread->setStopThread(true);
            thread->wait(10000);
            thread->deleteLater();
        }
    } catch (...) {
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Error while TranslateFromServer deleting thread";
    }
}
