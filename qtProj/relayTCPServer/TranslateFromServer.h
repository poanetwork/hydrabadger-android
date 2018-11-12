#ifndef TRANSLATEFROMSERVER_H
#define TRANSLATEFROMSERVER_H

#include <QObject>
#include <QStringList>
#include <QTcpServer>
#include <QTcpSocket>
#include <QList>
#include "TranslateFromServerThread.h"

class TranslateFromServer : public QTcpServer
{
    Q_OBJECT

public:
    TranslateFromServer(QObject *parent = nullptr);
    ~TranslateFromServer();

protected:
    void incomingConnection(qintptr socketDescriptor) override;

private slots:
    void deleteFromListThread();
    void deleteFromListThread(QAbstractSocket::SocketError);
    void deleteFromListThread(TranslateFromServerThread *thread);

private:
    QList<TranslateFromServerThread *> listOfThread;
};

#endif // TRANSLATEFROMSERVER_H
