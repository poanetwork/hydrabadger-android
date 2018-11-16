
#include <QtWidgets>
#include <QtNetwork>

#include "client.h"

//! [0]
Client::Client(QWidget *parent)
    : QDialog(parent)
    , hostCombo(new QComboBox)
    , portLineEdit(new QLineEdit)
    , getFortuneButton(new QPushButton(tr("Get Fortune")))
    , tcpSocket(new QTcpSocket(this))
{
    setWindowFlags(windowFlags() & ~Qt::WindowContextHelpButtonHint);
//! [0]
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

//! [1]
    in.setDevice(tcpSocket);
//! [1]

    connect(hostCombo, &QComboBox::editTextChanged,
            this, &Client::enableGetFortuneButton);
    connect(portLineEdit, &QLineEdit::textChanged,
            this, &Client::enableGetFortuneButton);
    connect(getFortuneButton, &QAbstractButton::clicked,
            this, &Client::requestNewFortune);
    connect(quitButton, &QAbstractButton::clicked, this, &QWidget::close);
//! [2] //! [3]
    connect(tcpSocket, &QIODevice::readyRead, this, &Client::readFortune);
//! [2] //! [4]
    connect(tcpSocket, QOverload<QAbstractSocket::SocketError>::of(&QAbstractSocket::error),
//! [3]
            this, &Client::displayError);
//! [4]

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

    portLineEdit->setText("3000");
//! [5]
}
//! [5]

//! [6]
void Client::requestNewFortune()
{
    getFortuneButton->setEnabled(false);
    tcpSocket->abort();
//! [7]
    tcpSocket->connectToHost(hostCombo->currentText(),
                             portLineEdit->text().toInt());

    QByteArray block;
    QDataStream out(&block, QIODevice::WriteOnly);
    out << 0xCAFECAFE;
    QString UID = "UID-UID-UID-UID-UID";
    qDebug()<<"Size "<<(qint32)UID.size();
    out << (qint32)UID.size();
    out.writeBytes(UID.toUtf8().data(), UID.size());
    tcpSocket->write(block);
//! [7]
}
//! [6]

void Client::waitForByte(QTcpSocket *socket, int size)
{
    const int Timeout = 60 * 1000;
    while(socket->bytesAvailable() < size){
        socket->waitForReadyRead(Timeout);
    }
}
//! [8]
void Client::readFortune()
{
    qDebug()<<"readFortune ";

    in.startTransaction();

    QString nextFortune;

    quint16 port1;
    quint16 port2;
    quint16 portmy;

    in >> port1;
    in >> port2;
    in >> portmy;

    // get size
    waitForByte(tcpSocket, sizeof (qint32));
    qint32 sizeOfStringArray;
    in >> sizeOfStringArray;
    qDebug()<<"sizeOfStringArray "<<sizeOfStringArray;
    // get MyIp
    waitForByte(tcpSocket, sizeOfStringArray);
    QByteArray ip;
    ip.resize(sizeOfStringArray);
    in >> ip;
    QString MyIp = QString::fromLocal8Bit(ip.data(), sizeOfStringArray);

    nextFortune = QString(" Port1 %1, Port2 %2, MyPort %3, MyIp %4");
    nextFortune = nextFortune.arg(port1).arg(port2).arg(portmy).arg(MyIp);

    if (!in.commitTransaction())
        return;

    currentFortune = nextFortune;
    statusLabel->setText(currentFortune);
    getFortuneButton->setEnabled(true);
}
//! [8]

//! [13]
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

