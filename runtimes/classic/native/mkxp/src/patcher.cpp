#include "config.h"
#include "debugwriter.h"
#include "patcher.h"
#include "sharedstate.h"
#include "json.h"

#include <fstream>
#include <regex>

Patcher::Patcher(std::vector<std::string> patches)
{
    for(std::string patch : patches)
        load(patch.c_str());
}

void Patcher::load(const char* path)
{
    try{
        int count = 0;

        nlohmann::json data;
        std::ifstream f(path);
        if(f.is_open())
        {
            data =  nlohmann::json::parse(f)["rpgm"].get<nlohmann::json>();
        }
        else
        {
            Debug()<<"Could not open patch file.";
        }

        for (auto it = data.begin(); it != data.end(); ++it)
        {
            nlohmann::json obj = *it;
            Patcher::Patch patch = {
                obj["key"].get<std::string>(),
                obj["value"].get<std::string>(),
            };
            patchList.push_back(patch);

            count++;
        }

        Debug()<<count<<" patches are added to patch list.";
    }
    catch (...)
    {
        Debug()<<"Could not read patches.";
    }
}

bool replaceStringInPlace(std::string& subject, const std::string& search,
                          const std::string& replace) {
    size_t pos = 0;
    int found = 0;
    while ((pos = subject.find(search, pos)) != std::string::npos)
    {
         subject.replace(pos, search.length(), replace);
         pos += replace.length();
         found++;
    }

    return found > 0;
}

void Patcher::apply(std::string& data)
{
    for ( const auto &p : patchList )
    {
        if(p.key.rfind("[regex]", 0) == 0)
        {
            std::string key = p.key.substr(7);
            std::regex re = std::regex(key);
            std::smatch m;
            if (std::regex_search(data, m, re))
            {
                data = std::regex_replace(data, re , p.value);

                Debug()<<"Applied patch: "<<p.key.c_str()<<" to "<<p.value.c_str();
            }
        }
        else if(replaceStringInPlace(data, p.key, p.value))
        {
            Debug()<<"Applied patch: "<<p.key.c_str()<<" to "<<p.value.c_str();
        }
    }
}
