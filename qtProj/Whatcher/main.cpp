#include <QCoreApplication>
#include <QSharedPointer>
#include <QAbstractSocket>
#include "client.h"

int main(int argc, char *argv[])
{
    QCoreApplication a(argc, argv);

    QProcessEnvironment env = QProcessEnvironment::systemEnvironment();
    env.insert("LD_LIBRARY_PATH", qApp->applicationDirPath());

    qRegisterMetaType<QAbstractSocket::SocketError>();

    QSharedPointer<Client> client = QSharedPointer<Client>(new Client());

    return a.exec();
}
