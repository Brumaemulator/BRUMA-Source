/*
** Modified version of vorbissource.cpp to support opus format
** Please check vorbissource.cpp for copyright and license information.
*/

#include "aldatasource.h"
#include "exception.h"

#include <opusfile.h>
#include <vector>
#include <algorithm>

const int BUFFER_SIZE = 11520;

static int opRead(void *ops, unsigned char* ptr, int nbytes)
{
	return SDL_RWread(static_cast<SDL_RWops*>(ops), ptr, sizeof(unsigned char), (size_t) nbytes);
}

static int opSeek(void *ops, opus_int64 offset, int whence)
{
	return SDL_RWseek(static_cast<SDL_RWops*>(ops), offset, whence) < 0 ? -1 : 0;
}

static opus_int64 opTell(void *ops)
{
	return SDL_RWtell(static_cast<SDL_RWops*>(ops));
}

static OpusFileCallbacks opCallbacks =
{
    opRead,
    opSeek,
    opTell,
    nullptr
};

struct OpusSource : ALDataSource
{
	SDL_RWops &src;

	OggOpusFile *of;

	uint32_t currentFrame;

	struct
	{
		uint32_t start;
		uint32_t length;
		uint32_t end;
		bool valid;
		bool requested;
	} loop;

	struct
	{
		int channels;
		int rate;
		int frameSize;
		ALenum alFormat;
	} info;

	std::vector<int16_t> sampleBuf;

	OpusSource(SDL_RWops &ops,
	             bool looped)
	    : src(ops),
	      currentFrame(0)
	{
		int error;
        of = op_open_callbacks(&src, &opCallbacks, nullptr, 0, &error);

		if (error)
		{
			SDL_RWclose(&src);
			throw Exception(Exception::MKXPError,
			                "Opusfile: Cannot read opus file");
		}

        const OpusHead* oh = op_head(of, -1);
        const OpusTags* ot = op_tags(of, -1);

		/* Extract bitstream info */
		info.channels = 2;
		info.rate = oh->input_sample_rate;

		info.alFormat = chooseALFormat(sizeof(int16_t), info.channels);
		info.frameSize = sizeof(int16_t) * info.channels;

		sampleBuf.resize(BUFFER_SIZE);

		loop.requested = looped;
		loop.valid = false;
		loop.start = loop.length = 0;

		if (!loop.requested)
			return;

		/* Try to extract loop info */
		for (int i = 0; i < ot->comments; ++i)
		{
			char *comment = ot->user_comments[i];
			char *sep = strstr(comment, "=");

			/* No '=' found */
			if (!sep)
				continue;

			/* Empty value */
			if (!*(sep+1))
				continue;

			*sep = '\0';

			if (!strcmp(comment, "LOOPSTART"))
				loop.start = strtol(sep+1, 0, 10);

			if (!strcmp(comment, "LOOPLENGTH"))
				loop.length = strtol(sep+1, 0, 10);

			*sep = '=';
		}

		loop.end = loop.start + loop.length;
		loop.valid = (loop.start && loop.length);
	}

	~OpusSource()
	{
		op_free(of);
		SDL_RWclose(&src);
	}

	int sampleRate()
	{
		return info.rate;
	}

	void seekToOffset(float seconds)
	{
		if (seconds <= 0)
		{
			op_raw_seek(of, 0);
			currentFrame = 0;
		}

		currentFrame = seconds * info.rate;

		if (loop.valid && currentFrame > loop.end)
			currentFrame = loop.start;

		/* If seeking fails, just seek back to start */
		if (op_pcm_seek(of, currentFrame) != 0)
			op_raw_seek(of, 0);
	}

	Status fillBuffer(AL::Buffer::ID alBuffer)
	{
		void *bufPtr = sampleBuf.data();
		int availBuf = sampleBuf.size();
		int bufUsed  = 0;

		int canRead = availBuf;

		Status retStatus = ALDataSource::NoError;

		bool readAgain = false;

		if (loop.valid)
		{
			int tilLoopEnd = loop.end * info.frameSize;

			canRead = std::min(availBuf, tilLoopEnd);
		}

		while (canRead > 16)
		{
			/* op_read_stereo downmixes samples to stereo so output always has two channels */
			long res = op_read_stereo(of, static_cast<int16_t*>(bufPtr), canRead);
			/* op_read_stereo returns number of samples read per channel so multiple it by 4*/
			res = res * 4;

			if (res < 0)
			{
				/* Read error */
				retStatus = ALDataSource::Error;

				break;
			}

			if (res == 0)
			{
				/* EOF */
				if (loop.requested)
				{
					retStatus = ALDataSource::WrapAround;
					seekToOffset(0);
				}
				else
				{
					retStatus = ALDataSource::EndOfStream;
				}

				/* If we sought right to the end of the file,
				 * we might be EOF without actually having read
				 * any data at all yet (which mustn't happen),
				 * so we try to continue reading some data. */
				if (bufUsed > 0)
					break;

				if (readAgain)
				{
					/* We're still not getting data though.
					 * Just error out to prevent an endless loop */
					retStatus = ALDataSource::Error;
					break;
				}

				readAgain = true;
			}

			bufUsed += (res / sizeof(int16_t));
			bufPtr = &sampleBuf[bufUsed];
			currentFrame += (res / info.frameSize);

			if (loop.valid && currentFrame >= loop.end)
			{
				/* Determine how many frames we're
				 * over the loop end */
				int discardFrames = currentFrame - loop.end;
				bufUsed -= discardFrames * info.channels;

				retStatus = ALDataSource::WrapAround;

				/* Seek to loop start */
				currentFrame = loop.start;
				if (op_pcm_seek(of, currentFrame) != 0)
					retStatus = ALDataSource::Error;

				break;
			}

			canRead -= res;
		}

		if (retStatus != ALDataSource::Error)
			AL::Buffer::uploadData(alBuffer, info.alFormat, sampleBuf.data(),
			                       bufUsed*sizeof(int16_t), info.rate);

		return retStatus;
	}

	uint32_t loopStartFrames()
	{
		if (loop.valid)
			return loop.start;
		else
			return 0;
	}

	bool setPitch(float)
	{
		return false;
	}
};

ALDataSource *createOpusSource(SDL_RWops &ops,
                                 bool looped)
{
	return new OpusSource(ops, looped);
}