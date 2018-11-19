#include <QtWidgets>
#include <QtNetwork>

#include "client.h"
#include <stdlib.h>

Client::Client(QWidget *parent)
    : QDialog(parent)
    , hostCombo(new QComboBox)
    , portLineEdit(new QLineEdit)
    , getFortuneButton(new QPushButton(tr("Get Fortune")))
    , tcpSocket(new QTcpSocket(this))
{
    setWindowFlags(windowFlags() & ~Qt::WindowContextHelpButtonHint);

    hostCombo->setEditable(true);
    // find out name of this machine
    QString name = QHostInfo::localHostName();
    if (!name.isEmpty()) {
        hostCombo->addItem(name);
        QString domain = QHostInfo::localDomainName();
        if (!domain.isEmpty())
            hostCombo->addItem(name + QChar('.') + domain);
    }
    if (name != QLatin1String("localhost"))
        hostCombo->addItem(QString("localhost"));
    // find out IP addresses of this machine
    QList<QHostAddress> ipAddressesList = QNetworkInterface::allAddresses();
    // add non-localhost addresses
    for (int i = 0; i < ipAddressesList.size(); ++i) {
        if (!ipAddressesList.at(i).isLoopback())
            hostCombo->addItem(ipAddressesList.at(i).toString());
    }
    // add localhost addresses
    for (int i = 0; i < ipAddressesList.size(); ++i) {
        if (ipAddressesList.at(i).isLoopback())
            hostCombo->addItem(ipAddressesList.at(i).toString());
    }
    hostCombo->addItem("62.176.10.54");

    portLineEdit->setValidator(new QIntValidator(1, 65535, this));

    auto hostLabel = new QLabel(tr("&Server name:"));
    hostLabel->setBuddy(hostCombo);
    auto portLabel = new QLabel(tr("S&erver port:"));
    portLabel->setBuddy(portLineEdit);

    statusLabel = new QLabel(tr("This examples requires that you run the "
                                "Fortune Server example as well."));

    getFortuneButton->setDefault(true);
    getFortuneButton->setEnabled(false);

    auto quitButton = new QPushButton(tr("Quit"));

    auto buttonBox = new QDialogButtonBox;
    buttonBox->addButton(getFortuneButton, QDialogButtonBox::ActionRole);
    buttonBox->addButton(quitButton, QDialogButtonBox::RejectRole);

    connect(hostCombo, &QComboBox::editTextChanged,
            this, &Client::enableGetFortuneButton);
    connect(portLineEdit, &QLineEdit::textChanged,
            this, &Client::enableGetFortuneButton);
    connect(getFortuneButton, &QAbstractButton::clicked,
            this, &Client::requestNewFortune);
    connect(quitButton, &QAbstractButton::clicked, this, &QWidget::close);

    connect(tcpSocket, &QIODevice::readyRead, this, &Client::read);

    in.setDevice(tcpSocket);

    QGridLayout *mainLayout = nullptr;
    if (QGuiApplication::styleHints()->showIsFullScreen() || QGuiApplication::styleHints()->showIsMaximized()) {
        auto outerVerticalLayout = new QVBoxLayout(this);
        outerVerticalLayout->addItem(new QSpacerItem(0, 0, QSizePolicy::Ignored, QSizePolicy::MinimumExpanding));
        auto outerHorizontalLayout = new QHBoxLayout;
        outerHorizontalLayout->addItem(new QSpacerItem(0, 0, QSizePolicy::MinimumExpanding, QSizePolicy::Ignored));
        auto groupBox = new QGroupBox(QGuiApplication::applicationDisplayName());
        mainLayout = new QGridLayout(groupBox);
        outerHorizontalLayout->addWidget(groupBox);
        outerHorizontalLayout->addItem(new QSpacerItem(0, 0, QSizePolicy::MinimumExpanding, QSizePolicy::Ignored));
        outerVerticalLayout->addLayout(outerHorizontalLayout);
        outerVerticalLayout->addItem(new QSpacerItem(0, 0, QSizePolicy::Ignored, QSizePolicy::MinimumExpanding));
    } else {
        mainLayout = new QGridLayout(this);
    }
    mainLayout->addWidget(hostLabel, 0, 0);
    mainLayout->addWidget(hostCombo, 0, 1);
    mainLayout->addWidget(portLabel, 1, 0);
    mainLayout->addWidget(portLineEdit, 1, 1);
    mainLayout->addWidget(statusLabel, 2, 0, 1, 2);
    mainLayout->addWidget(buttonBox, 3, 0, 1, 2);

    setWindowTitle(QGuiApplication::applicationDisplayName());
    portLineEdit->setFocus();

    QNetworkConfigurationManager manager;
    if (manager.capabilities() & QNetworkConfigurationManager::NetworkSessionRequired) {
        // Get saved network configuration
        QSettings settings(QSettings::UserScope, QLatin1String("QtProject"));
        settings.beginGroup(QLatin1String("QtNetwork"));
        const QString id = settings.value(QLatin1String("DefaultNetworkConfiguration")).toString();
        settings.endGroup();

        // If the saved network configuration is not currently discovered use the system default
        QNetworkConfiguration config = manager.configurationFromIdentifier(id);
        if ((config.state() & QNetworkConfiguration::Discovered) !=
            QNetworkConfiguration::Discovered) {
            config = manager.defaultConfiguration();
        }

        networkSession = new QNetworkSession(config, this);
        connect(networkSession, &QNetworkSession::opened, this, &Client::sessionOpened);

        getFortuneButton->setEnabled(false);
        statusLabel->setText(tr("Opening network session."));
        networkSession->open();
    }

    portLineEdit->setText("3002");
}

void Client::requestNewFortune()
{
    getFortuneButton->setEnabled(false);
    tcpSocket->abort();

    tcpSocket->connectToHost(hostCombo->currentText(),
                             portLineEdit->text().toInt());

    while(tcpSocket->state() != QAbstractSocket::ConnectedState) {
        QTime dieTime= QTime::currentTime().addSecs(1);
        while (QTime::currentTime() < dieTime)
            QCoreApplication::processEvents(QEventLoop::AllEvents, 10);
    }

    averageSpeed = 0;
    bytesWriteRead = 0;
    cntIter = 0;

    timer.start();
    session();
}


void Client::read()
{
    // get size
    waitForByte(tcpSocket, sizeof(int));
    QByteArray bytearay2;
    bytearay2.clear();
    bytearay2.resize(4096);

    qint32 size = 0;
    in >> size;
    bytearay2.resize(size);
    waitForByte(tcpSocket, size);
    size = in.readRawData(bytearay2.data(), size);

    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Read - "<<bytearay2.size()<<" bytes";

    QString UID2 = QString::fromLocal8Bit(bytearay2);

    cntIter++;
    bytesWriteRead += bytearay2.size();

    if(!(cntIter%100)) {
        qint64 ms = timer.elapsed();

        averageSpeed = (double)bytesWriteRead/((double)ms/1000.);

        bytesWriteRead = 0;
        timer.start();
    }

    if(UID2 == UID) {
        qDebug()<<"Live is good";

        QString str = QString("Success transmit and recive  - %1 bytes - averageSpeed - %2 bytes/sec").arg(size).arg(averageSpeed);
        statusLabel->setText(str);

//        int randTime = 200;
//        QTime dieTime= QTime::currentTime().addMSecs(randTime == 0 ? 100 : randTime);
//        while (QTime::currentTime() < dieTime)
//            QCoreApplication::processEvents(QEventLoop::AllEvents, 10);

        session();
    }
    else {
        qDebug()<<"Live is NOT good";
        qDebug()<<"bytearay2 - "<<bytearay2.size();

        QString str = QString("EROOR transmit - %1 bytes and recive  - %2 bytes").arg(UID.size()).arg(bytearay2.size());
        statusLabel->setText(str);

        QTime dieTime= QTime::currentTime().addSecs(5);
        while (QTime::currentTime() < dieTime)
            QCoreApplication::processEvents(QEventLoop::AllEvents, 10);

        session();
    }
}

void Client::session()
{
    QByteArray block;
    QDataStream out(&block, QIODevice::WriteOnly);

    UID = "UID";

//    int cnt = cntIter%1000;
    int cnt = qrand()%1000+1;

    for(int i = 0; i < cnt; i++)
        UID += "-UID";

    QByteArray bytearay = UID.toLocal8Bit();
    out.writeRawData(bytearay.data(), bytearay.size());

    qDebug()<<QDateTime::currentDateTime().toString("dd.MM.yyyy hh:mm:ss.zzz  --- ")<<" "<<"Write "<<(qint32)block.size();

    tcpSocket->write(block);
    tcpSocket->flush();
    tcpSocket->waitForBytesWritten();
}


void Client::waitForByte(QTcpSocket *socket, int size)
{
    while(socket->bytesAvailable() < size){
        socket->waitForReadyRead(100);

        QTime dieTime= QTime::currentTime().addMSecs(100);
        while (QTime::currentTime() < dieTime)
            QCoreApplication::processEvents(QEventLoop::AllEvents, 10);
    }
}

void Client::displayError(QAbstractSocket::SocketError socketError)
{
    switch (socketError) {
    case QAbstractSocket::RemoteHostClosedError:
        break;
    case QAbstractSocket::HostNotFoundError:
        QMessageBox::information(this, tr("Fortune Client"),
                                 tr("The host was not found. Please check the "
                                    "host name and port settings."));
        break;
    case QAbstractSocket::ConnectionRefusedError:
        QMessageBox::information(this, tr("Fortune Client"),
                                 tr("The connection was refused by the peer. "
                                    "Make sure the fortune server is running, "
                                    "and check that the host name and port "
                                    "settings are correct."));
        break;
    default:
        QMessageBox::information(this, tr("Fortune Client"),
                                 tr("The following error occurred: %1.")
                                 .arg(tcpSocket->errorString()));
    }

    getFortuneButton->setEnabled(true);
}
//! [13]

void Client::enableGetFortuneButton()
{
    getFortuneButton->setEnabled((!networkSession || networkSession->isOpen()) &&
                                 !hostCombo->currentText().isEmpty() &&
                                 !portLineEdit->text().isEmpty());

}

void Client::sessionOpened()
{
    // Save the used configuration
    QNetworkConfiguration config = networkSession->configuration();
    QString id;
    if (config.type() == QNetworkConfiguration::UserChoice)
        id = networkSession->sessionProperty(QLatin1String("UserChoiceConfiguration")).toString();
    else
        id = config.identifier();

    QSettings settings(QSettings::UserScope, QLatin1String("QtProject"));
    settings.beginGroup(QLatin1String("QtNetwork"));
    settings.setValue(QLatin1String("DefaultNetworkConfiguration"), id);
    settings.endGroup();

    statusLabel->setText(tr("This examples requires that you run the "
                            "Fortune Server example as well."));

    enableGetFortuneButton();
}

