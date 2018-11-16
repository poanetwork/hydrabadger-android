QT -= gui
QT += network

CONFIG += c++11 console
CONFIG -= app_bundle

DESTDIR = $$PWD/../RelayTCPServer_x64/

HEADERS       = dialog.h \
    LikeAStunServer.h \
    LikeAStunServerThread.h \
    Accessor.h \
    TranslateToServer.h \
    TranslateToServerThread.h \
    TranslateFromServer.h \
    TranslateFromServerThread.h \
    ThreadDeleter.h \
    StopServer.h \
    StopServerThread.h \
    Pinger.h \
    PingerThread.h
SOURCES       = dialog.cpp \
                main.cpp \
    LikeAStunServer.cpp \
    LikeAStunServerThread.cpp \
    Accessor.cpp \
    TranslateToServer.cpp \
    TranslateToServerThread.cpp \
    TranslateFromServer.cpp \
    TranslateFromServerThread.cpp \
    ThreadDeleter.cpp \
    StopServer.cpp \
    StopServerThread.cpp \
    Pinger.cpp \
    PingerThread.cpp


