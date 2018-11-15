#include "Pinger.h"

Pinger::Pinger(QObject *parent)
    : QTcpServer(parent)
{
    listOfThread.clear();
}

Pinger::~Pinger()
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Destructor Pinger";
    foreach(auto thread, listOfThread) {
        deleteFromListThread(thread);
        thread->wait(10);
        thread->deleteLater();
    }
}
//! [0]

//! [1]
void Pinger::incomingConnection(qintptr socketDescriptor)
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<" Pinger::incomingConnection";
    auto *thread = new PingerThread(socketDescriptor);
    listOfThread.append(thread);

    connect(thread, SIGNAL(finished()), this, SLOT(deleteFromListThread()));
    thread->start();

    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Thread Pinger start id - "<<thread->currentThreadId();
}

void Pinger::deleteFromListThread()
{
    PingerThread *thread;
    QObject* obj = QObject::sender();
    if (auto *tb = qobject_cast<PingerThread *>(obj)){
        thread = tb;
        deleteFromListThread(thread);
    }
}

void Pinger::deleteFromListThread(PingerThread *thread)
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Thread Pinger deleted id - "<<thread->currentThreadId();
    try {
        if(listOfThread.contains(thread)) {
            listOfThread.removeAt(listOfThread.indexOf(thread));
            thread->setStopThread(true);
            thread->wait(1000);
            thread->deleteLater();
        }
    } catch (...) {
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Erro Pinger while deleting thread";
    }
}
