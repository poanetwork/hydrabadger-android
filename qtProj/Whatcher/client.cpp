#include <QtNetwork>
#include <QSharedPointer>

#include "pinger.h"
#include "client.h"

Client::Client(QObject *parent)
    : QObject(parent)
{
    program = qApp->applicationDirPath();
    if(!program.endsWith(QDir::separator()))
        program += QDir::separator();
    program += "relayTCPServer";

    mPinger = QSharedPointer<pinger>(new pinger(this));
    QObject::connect(mPinger.data(), SIGNAL(notPinged()), this, SLOT(regenerateNewProc()));

//    QObject::connect(&timer, SIGNAL(timeout()), this, SLOT(regenerateNewProc()));
//    timer.start(60000);

    generateNewProc();
}

void Client::regenerateNewProc(QProcess::ProcessError err)
{
    if(proc.data()) {
        qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"regenerateNewProc "<<err;
        proc->kill();
        proc->terminate();
        proc.clear();
        mPinger->setStopThread(true);
        mPinger->wait(100);

        generateNewProc();
    }
}

void Client::regenerateNewProc()
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"ProcessError not pinged";

    QProcess::ProcessError er;
    regenerateNewProc(er);
}

void Client::generateNewProc()
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"generateNewProc - "<<program;

    QStringList env = QProcess::systemEnvironment();
    env << QString("LD_LIBRARY_PATH=%1").arg(qApp->applicationDirPath());
    env << QString("%1").arg(qApp->applicationDirPath());

    proc = QSharedPointer<QProcess>(new QProcess(this));
    proc->setProcessChannelMode(QProcess::MergedChannels);
    connect(proc.data(), SIGNAL(started()), this, SLOT(startPinged()));
    connect(proc.data(), SIGNAL(errorOccurred(QProcess::ProcessError)), this, SLOT(regenerateNewProc(QProcess::ProcessError)));

    connect (proc.data(), SIGNAL(readyReadStandardOutput()), this, SLOT(processOutput()));  // connect process signals with your code

    proc->setEnvironment(env);
    proc->start(program);
    qDebug()<<proc->readAllStandardOutput();
}

void Client::startPinged()
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"start Pinged thread";

    mPinger->setStopThread(false);
    mPinger->start();
}

void Client::processOutput()
{
    if(proc.data()) {
        QByteArray result = proc->readAllStandardOutput();
        QStringList lines = QString(result).split("\n");
        foreach (QString line, lines) {
            if(!line.isEmpty())
                qDebug()<<qPrintable( line );
        }
    }
}






