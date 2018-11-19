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
    void onReadyRead();

signals:
    void StopThreadChanged(bool StopThread);

    void getSocketWithDescriptor(qintptr socketDescriptor, bool fromto);
    void stopHandle(quint16 PORTFROMLISTEN, bool);

public slots:
    void setStopThread(bool StopThread);

    void initSocket(std::shared_ptr<QTcpSocket> tcpSocket);
    void unblock();

    void disconnect();

private:
    QByteArray data;
    bool    m_StopThread = false;

    quint16 localPort = 0;
    qintptr socketDescriptor;

    qint32 _blockSize   = 0;
    qint32 _blockSizeLast = 0;

    std::shared_ptr<QTcpSocket> tcpSocket;

    bool mIsinit{};
};

#endif // TRANSLATETOSERVERTHREAD_H
