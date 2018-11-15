#ifndef TRANSLATETOSERVERTHREAD_H
#define TRANSLATETOSERVERTHREAD_H

#include <QObject>
#include <QThread>
#include <QTcpSocket>
#include <QDataStream>

#include "Accessor.h"

class TranslateToServerThread : public QThread
{
    Q_OBJECT

    Q_PROPERTY(bool StopThread READ StopThread WRITE setStopThread NOTIFY StopThreadChanged)

public:
    explicit TranslateToServerThread(qintptr socketDescriptor, QObject *parent = nullptr);
    ~TranslateToServerThread();

    bool StopThread() const;
    void run() override;

private slots:
    void displayError(QAbstractSocket::SocketError socketError);
    void waitForByte(QTcpSocket *socket, int size);

signals:
    void StopThreadChanged(bool StopThread);

    void getSocketWithDescriptor(qintptr socketDescriptor, bool fromto);
    void stopHandle(quint16 PORTFROMLISTEN, bool);

    void sendDataFreedBack(quint16 PORTTOSend, const char *data, int len, qintptr socketDescriptor);
public slots:
    void setStopThread(bool StopThread);

    void initSocket(std::shared_ptr<QTcpSocket> tcpSocket);
    void unblock();

    void setUnblock();

    void disconnect();
private:
    qintptr socketDescriptor;
    bool m_StopThread;

    std::shared_ptr<QTcpSocket> tcpSocket;
    bool mIsinit{};

    //inputstream
    QDataStream in;

    bool mIsBlockSend{};
};

#endif // TRANSLATETOSERVERTHREAD_H
