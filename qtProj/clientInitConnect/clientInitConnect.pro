QT += network widgets
requires(qtConfig(combobox))

DESTDIR = $$PWD/../RelayTCPServer_x64/

HEADERS       = client.h
SOURCES       = client.cpp \
                main.cpp
