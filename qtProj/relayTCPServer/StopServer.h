#ifndef STOPSERVER_H
#define STOPSERVER_H

#include <QObject>
#include <QStringList>
#include <QTcpServer>
#include <QTcpSocket>
#include <QList>

#include "StopServerThread.h"

//! [0]
class StopServer : public QTcpServer
{
    Q_OBJECT

public:
    StopServer(QObject *parent = nullptr);
    ~StopServer();

protected:
    void incomingConnection(qintptr socketDescriptor) override;

private slots:
    void deleteFromListThread();
    void deleteFromListThread(QAbstractSocket::SocketError);
    void deleteFromListThread(StopServerThread *thread);

private:
    QList<StopServerThread *> listOfThread;
};
//! [0]



#endif // STOPSERVER_H
