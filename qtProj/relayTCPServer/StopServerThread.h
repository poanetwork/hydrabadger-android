#ifndef STOPSERVERTHREAD_H
#define STOPSERVERTHREAD_H

#include <QObject>
#include <QObject>
#include <QThread>
#include <QTcpSocket>
#include <QDataStream>
#include <memory>

#include "Accessor.h"

class StopServerThread : public QThread
{
    Q_OBJECT

    Q_PROPERTY(bool StopThread READ StopThread WRITE setStopThread NOTIFY StopThreadChanged)

public:
    explicit StopServerThread(int socketDescriptor, QObject *parent = nullptr);
    ~StopServerThread();

    bool StopThread() const;
    void run() override;

private slots:
    void displayError(QAbstractSocket::SocketError socketError);

signals:
    void error(QAbstractSocket::SocketError socketError);
    void StopThreadChanged(bool StopThread);

    void stopHandleWithUID(QString UID);
public slots:
    void setStopThread(bool StopThread);


    void setUnblock();
private slots:
    void waitForByte(QTcpSocket &socket, int size);

private:
    int socketDescriptor;
    bool m_StopThread;

    //inputstream
    QDataStream in;

    bool mIsBlock{};
};

#endif // STOPSERVERTHREAD_H
