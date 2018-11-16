#ifndef THREADDELETER_H
#define THREADDELETER_H

#include <QObject>
#include <QThread>
#include <QTcpSocket>
#include <QMutex>
#include <QHash>
#include <QList>
#include <QTime>
#include <QMutexLocker>

#include "Accessor.h"

class ThreadDeleter : public QThread
{
    Q_OBJECT

    Q_PROPERTY(bool StopThread READ StopThread WRITE setStopThread NOTIFY StopThreadChanged)

public:
    explicit ThreadDeleter(QObject *parent = nullptr);
    ~ThreadDeleter();

    bool StopThread() const;
    void run() override;

signals:
    void StopThreadChanged(bool StopThread);

    void stopHandle(quint16 PORTFROMLISTEN);
public slots:

    void setStopThread(bool StopThread);
private:
    bool m_StopThread;
};


#endif // THREADDELETER_H
