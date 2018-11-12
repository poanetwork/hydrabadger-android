#ifndef FORTUNESERVER_H
#define FORTUNESERVER_H

#include <QStringList>
#include <QTcpServer>
#include <QList>
#include "LikeAStunServerThread.h"

//! [0]
class LikeAStunServer : public QTcpServer
{
    Q_OBJECT

public:
    LikeAStunServer(QObject *parent = nullptr);
    ~LikeAStunServer();

protected:
    void incomingConnection(qintptr socketDescriptor) override;

private slots:
    void deleteFromListThread();
    void deleteFromListThread(QAbstractSocket::SocketError);
    void deleteFromListThread(LikeAStunServerThread *thread);

private:
    QList<LikeAStunServerThread *> listOfThread;
};
//! [0]

#endif
