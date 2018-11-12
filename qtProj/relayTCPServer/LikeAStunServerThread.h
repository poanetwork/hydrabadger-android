#ifndef FORTUNETHREAD_H
#define FORTUNETHREAD_H

#include <QThread>
#include <QTcpSocket>
#include <QDataStream>
#include <QTime>

#include "Accessor.h"

using Ports = std::pair<quint16, quint16>;
static quint32 MAGIC_NUMBER = (quint32)0xCAFECAFE;

//! [0]
class LikeAStunServerThread : public QThread
{
    Q_OBJECT

    Q_PROPERTY(bool StopThread READ StopThread WRITE setStopThread NOTIFY StopThreadChanged)

public:
    LikeAStunServerThread(int socketDescriptor, QObject *parent = nullptr);
    ~LikeAStunServerThread();

    void run() override;
    bool StopThread() const;

public slots:
    void setStopThread(bool StopThread);
    void waitForByte(QTcpSocket &socket, int size);

    void beenInitedHandle(Ports port);

private slots:
    void displayError(QAbstractSocket::SocketError socketError);

signals:
    void error(QAbstractSocket::SocketError socketError);
    void StopThreadChanged(bool StopThread);

    void initHandle(QString UserGlobalUID, QString UserGlobalIP, quint16 UserGlobalPORT, quint64 secsSinceStartBindRequest);
private:
    int socketDescriptor;
    bool mStopThread;

    bool mIsinitHandle{};
    Ports ports;

    //inputstream
    QDataStream in;
};
//! [0]

#endif
