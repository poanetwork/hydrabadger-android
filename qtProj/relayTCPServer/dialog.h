#ifndef DIALOG_H
#define DIALOG_H

#include <QObject>
#include "LikeAStunServer.h"
#include "ThreadDeleter.h"
#include "StopServer.h"
#include <csignal>

class Dialog : public QObject
{
    Q_OBJECT

public:
    Dialog(QObject *parent = nullptr);
    ~Dialog();

public slots:
    void deleteMe();

private:
    static void exitQt(int _);

private:
    LikeAStunServer server;
    StopServer mStopServer;

    // waiter 3 min
    QSharedPointer<ThreadDeleter> threadDeleter;
};

#endif
