#ifndef PINGER_H
#define PINGER_H

#include <QObject>
#include <QThread>
#include <QtCore>
#include <QTcpSocket>
#include <QHostAddress>


class pinger : public QThread
{
    Q_OBJECT

    Q_PROPERTY(bool StopThread READ StopThread WRITE setStopThread NOTIFY StopThreadChanged)

public:
    explicit pinger(QObject *parent = nullptr);
    ~pinger();

    bool StopThread() const;
    void run() override;

signals:
    void StopThreadChanged(bool StopThread);

    void notPinged();
public slots:
    void setStopThread(bool StopThread);

    void waitForByte(QTcpSocket *socket, int size);
    void error(QAbstractSocket::SocketError socketError);
private:
    bool m_StopThread;
    QHostAddress ipAddress;

    QSharedPointer<QTcpSocket> tcpSocket;
    QDataStream in;
};

#endif // PINGER_H
