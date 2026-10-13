#ifndef __HTTP_H__
#define __HTTP_H__

#include <string>

#include <functional>
#include <map>


class HTTP
{
public:
	static std::string get(std::string host, std::string query);
	static std::string post(std::string host, std::string query, std::map<std::string, std::string> requestBody);
	static int download(std::string host, std::string query, std::string path, std::function<void(long current, long total)> onProgress);
};

#endif