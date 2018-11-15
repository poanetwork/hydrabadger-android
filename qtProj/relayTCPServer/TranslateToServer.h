#ifndef TRANSLATETOSERVER_H
#define TRANSLATETOSERVER_H

#include <QObject>
#include <QStringList>
#include <QTcpServer>
#include <QTcpSocket>
#include <QList>
#include "TranslateToServerThread.h"

//! [0]
class TranslateToServer : public QTcpServer
{
    Q_OBJECT

public:
    TranslateToServer(QObject *parent = 0);
    ~TranslateToServer();

protected:
    void incomingConnection(qintptr socketDescriptor) override;

private slots:
    void deleteFromListThread();
    void deleteFromListThread(TranslateToServerThread *thread);

private:
    QList<TranslateToServerThread *> listOfThread;
};


#endif // TRANSLATETOSERVER_H
