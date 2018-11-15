
#ifndef CLIENT_H
#define CLIENT_H

#include <QDataStream>
#include <QtCore>
#include <QTcpSocket>
#include <QNetworkSession>
#include <QProcess>
#include <QDir>

#include "pinger.h"

//! [0]
class Client : public QObject
{
    Q_OBJECT

public:
    explicit Client(QObject *parent = nullptr);

private slots:
    void regenerateNewProc(QProcess::ProcessError err);
    void regenerateNewProc();
    void generateNewProc();

    void startPinged();

    void processOutput();
private:
    QSharedPointer<QProcess> proc;
    QSharedPointer<pinger>  mPinger;

    QString program;
};
//! [0]

#endif
