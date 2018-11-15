#ifndef PINGER_H
#define PINGER_H

#include <QObject>
#include <QStringList>
#include <QTcpServer>
#include <QTcpSocket>
#include <QDateTime>
#include <QThread>
#include <QList>

#include "PingerThread.h"

//! [0]
class Pinger : public QTcpServer
{
    Q_OBJECT

public:
    Pinger(QObject *parent = nullptr);
    ~Pinger();

protected:
    void incomingConnection(qintptr socketDescriptor) override;

private slots:
    void deleteFromListThread();
    void deleteFromListThread(PingerThread *thread);

private:
    QList<PingerThread *> listOfThread;
};
//! [0]

#endif // PINGER_H
