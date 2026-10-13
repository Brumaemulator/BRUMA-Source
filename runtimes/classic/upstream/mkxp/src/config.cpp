/*
** config.cpp
**
** This file is part of mkxp.
**
** Copyright (C) 2013 Jonas Kulla <Nyocurio@gmail.com>
**
** mkxp is free software: you can redistribute it and/or modify
** it under the terms of the GNU General Public License as published by
** the Free Software Foundation, either version 2 of the License, or
** (at your option) any later version.
**
** mkxp is distributed in the hope that it will be useful,
** but WITHOUT ANY WARRANTY; without even the implied warranty of
** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
** GNU General Public License for more details.
**
** You should have received a copy of the GNU General Public License
** along with mkxp.  If not, see <http://www.gnu.org/licenses/>.
*/

#include "config.h"

#include <SDL_filesystem.h>

#include <fstream>
#include <stdint.h>

#include "android.h"
#include "debugwriter.h"
#include "util.h"
#include "sdl-util.h"
#include "iniconfig.h"
#include "json.h"

#ifdef INI_ENCODING
extern "C" {
#include <libguess.h>
}
#include <iconv.h>
#include <errno.h>
#endif

/* http://stackoverflow.com/a/1031773 */
static bool validUtf8(const char *string)
{
	const uint8_t *bytes = (uint8_t*) string;

	while(*bytes)
	{
		if( (/* ASCII
			  * use bytes[0] <= 0x7F to allow ASCII control characters */
				bytes[0] == 0x09 ||
				bytes[0] == 0x0A ||
				bytes[0] == 0x0D ||
				(0x20 <= bytes[0] && bytes[0] <= 0x7E)
			)
		) {
			bytes += 1;
			continue;
		}

		if( (/* non-overlong 2-byte */
				(0xC2 <= bytes[0] && bytes[0] <= 0xDF) &&
				(0x80 <= bytes[1] && bytes[1] <= 0xBF)
			)
		) {
			bytes += 2;
			continue;
		}

		if( (/* excluding overlongs */
				bytes[0] == 0xE0 &&
				(0xA0 <= bytes[1] && bytes[1] <= 0xBF) &&
				(0x80 <= bytes[2] && bytes[2] <= 0xBF)
			) ||
			(/* straight 3-byte */
				((0xE1 <= bytes[0] && bytes[0] <= 0xEC) ||
					bytes[0] == 0xEE ||
					bytes[0] == 0xEF) &&
				(0x80 <= bytes[1] && bytes[1] <= 0xBF) &&
				(0x80 <= bytes[2] && bytes[2] <= 0xBF)
			) ||
			(/* excluding surrogates */
				bytes[0] == 0xED &&
				(0x80 <= bytes[1] && bytes[1] <= 0x9F) &&
				(0x80 <= bytes[2] && bytes[2] <= 0xBF)
			)
		) {
			bytes += 3;
			continue;
		}

		if( (/* planes 1-3 */
				bytes[0] == 0xF0 &&
				(0x90 <= bytes[1] && bytes[1] <= 0xBF) &&
				(0x80 <= bytes[2] && bytes[2] <= 0xBF) &&
				(0x80 <= bytes[3] && bytes[3] <= 0xBF)
			) ||
			(/* planes 4-15 */
				(0xF1 <= bytes[0] && bytes[0] <= 0xF3) &&
				(0x80 <= bytes[1] && bytes[1] <= 0xBF) &&
				(0x80 <= bytes[2] && bytes[2] <= 0xBF) &&
				(0x80 <= bytes[3] && bytes[3] <= 0xBF)
			) ||
			(/* plane 16 */
				bytes[0] == 0xF4 &&
				(0x80 <= bytes[1] && bytes[1] <= 0x8F) &&
				(0x80 <= bytes[2] && bytes[2] <= 0xBF) &&
				(0x80 <= bytes[3] && bytes[3] <= 0xBF)
			)
		) {
			bytes += 4;
			continue;
		}

		return false;
	}

	return true;
}

static std::string prefPath(const char *org, const char *app)
{
	char *path = SDL_GetPrefPath(org, app);

	if (!path)
		return std::string();

	std::string str(path);
	SDL_free(path);

	return str;
}

template<typename T>
std::set<T> setFromVec(const std::vector<T> &vec)
{
	return std::set<T>(vec.begin(), vec.end());
}

typedef std::vector<std::string> StringVec;
#ifdef __ANDROID__
#define CONF_FILE androidGetConfigFile()
#else
#define CONF_FILE "mkxp.json"
#endif

Config::Config()
{}

void Config::read(int argc, char *argv[])
{

// Not gonna take your shit boost
#define GUARD_ALL( exp ) try { exp } catch(...) {}

	editor.debug = false;
	editor.battleTest = false;

	/* Read arguments sent from the editor */
	if (argc > 1)
	{
		std::string argv1 = argv[1];
		/* RGSS1 uses "debug", 2 and 3 use "test" */
		if (argv1 == "debug" || argv1 == "test")
			editor.debug = true;
		else if (argv1 == "btest")
			editor.battleTest = true;

		/* Fix offset */
		if (editor.debug || editor.battleTest)
		{
			argc--;
			argv++;
		}
	}

	/* Parse configuration file */
	SDLRWStream confFile(CONF_FILE, "r");

	if (confFile)
	{
		try
		{
			nlohmann::json configData = nlohmann::json::parse(confFile.stream());

#define JSON_GET(key, def) 				\
	if(configData.contains(#key))		\
		configData[#key].get_to(key); 	\
	else 								\
		key = def;

			JSON_GET(rgssVersion, 0);
			JSON_GET(debugMode, false);
			JSON_GET(cheats, false);
			JSON_GET(enablePostloadScripts, true);
			JSON_GET(hideMKXP, false);
			JSON_GET(printFPS, false);
			JSON_GET(winResizable, false);
			JSON_GET(fullscreen, false);
			JSON_GET(fixedAspectRatio, true);
			JSON_GET(smoothScaling, true);
			JSON_GET(vsync, false);
			JSON_GET(defScreenW, 0);
			JSON_GET(defScreenH, 0);
			JSON_GET(windowTitle, "");
			JSON_GET(fixedFramerate, 0);
			JSON_GET(fastForward, false);
			JSON_GET(fastForwardSpeed, 1);
			JSON_GET(frameSkip, false);
			JSON_GET(syncToRefreshrate, false);
			JSON_GET(verticalScreenAlign, "center");
			JSON_GET(fontScale, 0.7);
			JSON_GET(customFont, "");
			JSON_GET(solidFonts, false);
			JSON_GET(subImageFix, false);
			JSON_GET(enableBlitting, true);
			JSON_GET(maxTextureSize, 0);
			JSON_GET(gameFolder, ".");
			JSON_GET(copyText, false);
			JSON_GET(anyAltToggleFS, false);
			JSON_GET(enableReset, true);
			JSON_GET(allowSymlinks, false);
			JSON_GET(dataPathOrg, "");
			JSON_GET(dataPathApp, "");
			JSON_GET(iconPath, "");
			JSON_GET(execName, "Game");
			JSON_GET(titleLanguage, "");
			JSON_GET(midi.soundFont, "");
			JSON_GET(midi.chorus, false);
			JSON_GET(midi.reverb, false);
			JSON_GET(SE.sourceCount, 6);
			JSON_GET(pathCache, true);
			JSON_GET(prebuiltPathCache, true);
			JSON_GET(fastPathEnum, true);
			JSON_GET(useUnpackedScripts, false);
			JSON_GET(customScript, "");
			JSON_GET(useScriptNames, false);
			JSON_GET(preloadScripts, preloadScripts);
			JSON_GET(postloadScripts, postloadScripts);
			JSON_GET(rtps, rtps);
			JSON_GET(patches, patches);
			JSON_GET(fontSubs, fontSubs);
			JSON_GET(SDLControllerMappings, SDLControllerMappings);
			JSON_GET(rubyLoadpaths, rubyLoadpaths);

#undef JSON_GET

		}
		catch (nlohmann::detail::parse_error &error)
		{
			Debug() << CONF_FILE << ":" << error.what();
		}
	}

	rgssVersion = clamp(rgssVersion, 0, 3);

	SE.sourceCount = clamp(SE.sourceCount, 1, 64);

	if (!dataPathOrg.empty() && !dataPathApp.empty())
		customDataPath = prefPath(dataPathOrg.c_str(), dataPathApp.c_str());

	commonDataPath = prefPath(".", "mkxp");
}

static std::string baseName(const std::string &path)
{
	size_t pos = path.find_last_of("/\\");

	if (pos == path.npos)
		return path;

	return path.substr(pos + 1);
}

static void setupScreenSize(Config &conf)
{
	if (conf.defScreenW <= 0)
		conf.defScreenW = (conf.rgssVersion == 1 ? 640 : 544);

	if (conf.defScreenH <= 0)
		conf.defScreenH = (conf.rgssVersion == 1 ? 480 : 416);
}



bool convertIfNotValidUTF8(const char* lang, std::string str)
{
#ifdef INI_ENCODING
    /* Can add more later */
    const char *languages[] =
    {
        lang,
        GUESS_REGION_JP, /* Japanese */
        GUESS_REGION_KR, /* Korean */
        GUESS_REGION_CN, /* Chinese */
        0
    };

    bool convSuccess = true;

	/* Verify that the string is UTF-8, and if not,
	 * try to determine the encoding and convert to UTF-8 */
	if (!validUtf8(str.c_str()))
	{
		const char *encoding = 0;
		convSuccess = false;

		for (size_t i = 0; languages[i]; ++i)
		{
			encoding = libguess_determine_encoding(str.c_str(),
			                                       sizeof(str.size()),
			                                       languages[i]);
			if (encoding)
				break;
		}

		if (encoding)
		{
			iconv_t cd = iconv_open("UTF-8", encoding);

			size_t inLen = str.size();
			size_t outLen = inLen * 4;
			std::string buf(outLen, '\0');
			char *inPtr = const_cast<char*>(str.c_str());
			char *outPtr = const_cast<char*>(buf.c_str());

			errno = 0;
			size_t result = iconv(cd, &inPtr, &inLen, &outPtr, &outLen);

			iconv_close(cd);

			if (result != (size_t) -1 && errno == 0)
			{
				buf.resize(buf.size()-outLen);
				str = buf;
				convSuccess = true;
			}
		}
	}
    return convSuccess;
#endif

	return false;
}

void Config::toggleFastForward()
{
    fastForward = !fastForward;
}

void Config::readGameINI()
{
	if (!customScript.empty())
	{
		game.title = baseName(customScript);

		if (rgssVersion == 0)
			rgssVersion = 1;

		setupScreenSize(*this);

		return;
	}

	std::string iniFilename = execName + ".ini";
	SDLRWStream iniFile(iniFilename.c_str(), "r");

	if (iniFile)
	{
		INIConfiguration ic;
		if(ic.load(iniFile.stream()))
		{
			GUARD_ALL( game.title = ic.getStringProperty("Game", "Title"); );
			GUARD_ALL( game.scripts = ic.getStringProperty("Game", "Scripts"); );

			strReplace(game.scripts, '\\', '/');

			if (game.title.empty())
			{
				Debug() << iniFilename + ": Could not find Game.Title property";
			}

			if (game.scripts.empty())
			{
				Debug() << iniFilename + ": Could not find Game.Scripts property";
			}
		}
		else
		{
			Debug() << iniFilename + ": Failed to parse ini file";
		}
	}
	else
	{
		Debug() << "FAILED to open" << iniFilename;
	}

#ifdef INI_ENCODING
	if (!convertIfNotValidUTF8(titleLanguage.c_str(), game.title))
		game.title.clear();
#else
	if (!validUtf8(game.title.c_str()))
		game.title.clear();
#endif

	if (game.title.empty())
		game.title = baseName(gameFolder);

	if (rgssVersion == 0)
	{
		/* Try to guess RGSS version based on Data/Scripts extension */
		rgssVersion = 1;

		if (!game.scripts.empty())
		{
			const char *p = &game.scripts[game.scripts.size()];
			const char *head = &game.scripts[0];

			while (--p != head)
				if (*p == '.')
					break;

			if (!strcmp(p, ".rvdata"))
				rgssVersion = 2;
			else if (!strcmp(p, ".rvdata2"))
				rgssVersion = 3;
		}
	}

	setupScreenSize(*this);
}
