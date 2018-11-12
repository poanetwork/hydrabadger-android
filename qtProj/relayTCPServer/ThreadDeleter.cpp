#include "ThreadDeleter.h"
#include <QCoreApplication>

ThreadDeleter::ThreadDeleter(QObject *parent)
    : QThread(parent)
{
    m_StopThread = false;
}

ThreadDeleter::~ThreadDeleter()
{
    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<"Destructor ThreadDeleter";

    setStopThread(true);
//    QThread::terminate();
    QThread::wait();
}


bool ThreadDeleter::StopThread() const
{
    return m_StopThread;
}


void ThreadDeleter::setStopThread(bool StopThread)
{
    if (m_StopThread == StopThread)
        return;

    m_StopThread = StopThread;
    emit StopThreadChanged(m_StopThread);
}

void ThreadDeleter::run()
{
    QObject::connect(this,
            SIGNAL(stopHandle(quint16)), Accessor::getInstance(), SLOT(stopHandle(quint16)));

    forever {
        if(m_StopThread)
            break;

        QList<quint16> keys = Accessor::getInstance()->AllConnectHandles.keys();

        foreach (quint16 portfrom, keys) {
            if(m_StopThread)
                break;

            QTime time = QTime::currentTime();
            auto timeFrom = (quint64)((quint64)time.msecsSinceStartOfDay()/(quint64)1000);
            // wait 3 min
            timeFrom -= 3*60;

            // exist
            if(Accessor::getInstance()->AllConnectHandles.contains(portfrom)) {
                // not binded at 3 min
                if(!Accessor::getInstance()->AllConnectHandles.value(portfrom)->wasBinded &&
                        timeFrom > Accessor::getInstance()->AllConnectHandles.value(portfrom)->secsSinceStartBindRequest) {
                    qDebug()<<"ThreadDeleter found not binded Port after 3 min portfrom - "<< portfrom;
                    ////Event LOOP
                    emit stopHandle(portfrom);
                }
                // delete not active after 3 min
                else if(Accessor::getInstance()->GetSocketTo(portfrom, true) != nullptr)
                {
                    if(Accessor::getInstance()->GetSocketTo(portfrom, true)->state() != QTcpSocket::ConnectedState &&
                            timeFrom > Accessor::getInstance()->AllConnectHandles.value(portfrom)->secsSinceStartBindRequest) {
                        qDebug()<<"ThreadDeleter found not active Port after 3 min portfrom - "<< portfrom;
                        ////Event LOOP
                        emit stopHandle(portfrom);
                    }
                }
                else if(Accessor::getInstance()->GetSocketTo(portfrom, true) == nullptr &&
                        timeFrom > Accessor::getInstance()->AllConnectHandles.value(portfrom)->secsSinceStartBindRequest)
                {
                    qDebug()<<"ThreadDeleter found NULL Port portfrom - "<< portfrom;
                    ////Event LOOP
                    emit stopHandle(portfrom);
                }
            }
            else {
                qCritical()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"ThreadDeleter  AllConnectHandles NOT contains portfrom "<<portfrom;
            }
        }

        msleep(100);
    }
}
