
#define CPPHTTPLIB_OPENSSL_SUPPORT 1
#define CPPHTTPLIB_ZLIB_SUPPORT 1

#include <iostream>
#include <fstream>
#include <string>

#include "httplib.h"
#include "http.h"
#include "config.h"
#include "sharedstate.h"
#include "debugwriter.h"

std::string HTTP::get(std::string host, std::string query)
{
	httplib::Client cli(host);
	cli.enable_server_certificate_verification(false);

	auto res = cli.Get(query);
	if (res)
	{
		return res->body;
	}
	else
	{
		auto err = res.error();
        Debug() <<"HTTP error: "<<httplib::to_string(err);

		return "";
	}
}

std::string HTTP::post(std::string host, std::string query, std::map<std::string, std::string> requestBody)
{

	httplib::Client cli(host);
	cli.enable_server_certificate_verification(false);

	httplib::Params params;

	for (auto const& [key, val] : requestBody)
	{
		params.emplace(key, val);
	}

	auto res = cli.Post(query, params);

	if (res)
	{
		return res->body;

	}
	else
	{
		auto err = res.error();
        Debug()<<"HTTP error: "<<httplib::to_string(err);

		return "";
	}
}

int HTTP::download(std::string host, std::string query, std::string path, std::function<void(long current, long total)> onProgress)
{
	httplib::Client cli(host);
	cli.enable_server_certificate_verification(false);

	int status = -1;

	std::string filePath = shState->config().gameFolder+std::string("/")+path;

	std::ofstream fstream;
	fstream.open(filePath.c_str(), std::ios::binary);

	if (!fstream.is_open())
		return -1;

	auto res = cli.Get(
		query,
  		[&](const char *data, size_t data_length) {
  			fstream.write(data, data_length);
    		return true;
  		},
  		[&](long current, long total) {
      		onProgress(current, total);
      		return true;
    	}
  	);

	if(res)
	{
		status = res->status;
	}
  	else
  	{
  		auto err = res.error();
        Debug()<<"HTTP error: "<<httplib::to_string(err);
  	}

  	fstream.flush();
  	fstream.close();

  	return status;
}