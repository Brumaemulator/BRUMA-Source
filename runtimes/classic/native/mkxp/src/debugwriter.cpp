#include <fstream>
#include <string>

#include "debugwriter.h"
#include "config.h"
#include "sharedstate.h"
#include "android.h"

#ifdef _WIN32
#define LOGFILE std::string("\\logs.txt")
#else
#define LOGFILE std::string("/logs.txt")
#endif

void deleteLogFile(std::string gameFolder)
{
    std::string logFilePath = gameFolder + LOGFILE;
    std::remove(logFilePath.c_str());
}

Debug::~Debug()
{
    std::string message = buf.str();

#ifdef INI_ENCODING
    convertIfNotValidUTF8("", message);
#endif

#ifdef __ANDROID__
    androidPrint(message.c_str());
#else
    std::cerr << message.c_str() << std::endl;
#endif

    if(!shState) return;
    std::string logFilePath = std::string(shState->config().gameFolder) + LOGFILE;
    std::ofstream logFile;
    logFile.open(logFilePath.c_str(), std::ios_base::app);
    logFile << message.c_str() << std::endl;
    logFile.close();
}