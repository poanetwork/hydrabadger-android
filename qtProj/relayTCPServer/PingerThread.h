#ifndef PINGERTHREAD_H
#define PINGERTHREAD_H

#include <QThread>
#include <QTcpSocket>
#include <QDataStream>
#include <QHostAddress>
#include <QTime>
#include "LikeAStunServerThread.h"

class PingerThread : public QThread
{
    Q_OBJECT

    Q_PROPERTY(bool StopThread READ StopThread WRITE setStopThread NOTIFY StopThreadChanged)

public:
    explicit PingerThread(qintptr socketDescriptor, QObject *parent = nullptr);

    void run() override;

    bool StopThread() const;

public slots:
    void setStopThread(bool StopThread);

signals:
    void StopThreadChanged(bool StopThread);

private slots:
    void displayError(QAbstractSocket::SocketError socketError);
    void waitForByte(QTcpSocket &socket, int size);

private:
    qintptr socketDescriptor;

    //inputstream
    QDataStream in;
    bool m_StopThread;
};

#endif // PINGERTHREAD_H
