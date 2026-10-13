#ifndef PATCHER_H
#define PATCHER_H

#include <string>
#include <vector>

class Patcher
{
public:

    struct Patch
    {
        std::string key;
        std::string value;
    };

	Patcher(std::vector<std::string> patches);
    void apply(std::string& data);

private:
    void load(const char* path);
    std::vector<Patch> patchList;
};

#endif // PATCHER_H