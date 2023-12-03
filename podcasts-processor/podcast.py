#!/usr/bin/env python3

from pydub import AudioSegment

import utils
from utils import *

logger = logging.getLogger("pydub.converter")
logger.setLevel(logging.DEBUG)
logger.addHandler(logging.StreamHandler())


def create_podcast(
        asset_intro_file: str,
        asset_music_segue_file: str,
        asset_closing_file: str,
        episode_intro_file: str,
        episode_interview_file: str,
        output_directory: str
):
    assert os.path.exists(output_directory) or reset_and_recreate_directory(
        output_directory
    ), ("the directory %s does not exist " "and couldn't be created" % output_directory)

    return_value = {}
    audio_extension: str = 'mp3'
    intro = AudioSegment.from_file(asset_intro_file, format=audio_extension)
    first_segue = AudioSegment.from_file(asset_music_segue_file, format=audio_extension)
    closing = AudioSegment.from_file(asset_closing_file, format=audio_extension)
    host_intro = AudioSegment.from_file(episode_intro_file, format=audio_extension)
    interview = AudioSegment.from_file(episode_interview_file, format=audio_extension)
    out = (
        intro.append(host_intro, crossfade=10 * 1000)
        .append(first_segue, crossfade=10 * 1000)
        .append(interview, crossfade=5 * 1000)
        .append(closing, crossfade=5 * 1000)
    )

    # for ext in output_formats:

    utils.log("about to write file of type %s" % audio_extension)
    output_file_name = os.path.join(output_directory, f'podcast.{audio_extension}')
    utils.log("exporting to %s" % output_file_name)
    out.export(output_file_name, format=audio_extension, bitrate="256k")
    assert os.path.exists(output_file_name), "the .%s file should've been created at %s" % (
        audio_extension, output_file_name)
    return_value['export'] = output_file_name

    utils.log(
        "the output directory's size is %s " % os.path.getsize(output_directory)
    )

    return return_value


if __name__ == "__main__":
    import os
    import os.path


    def valid_path_env_var(k: str) -> str:
        assert k is not None and k in os.environ, (
                "there is no environment variable called %s" % k
        )
        value: str = os.path.expanduser(os.environ.get(k))
        assert os.path.exists(value), (
                "the directory pointed to by %s does not exist" % k
        )
        return value


    assets_dir: str = valid_path_env_var("PODCAST_ASSETS_DIR")
    output_dir: str = valid_path_env_var("PODCAST_OUTPUT_DIR")
    input_dir: str = valid_path_env_var("PODCAST_INPUT_DIR")
    interview_wav: str = os.path.join(input_dir, "interview.wav")
    intro_wav: str = os.path.join(input_dir, "intro.wav")
    asset_segue_music: str = os.path.join(assets_dir, "music-segue.wav")
    asset_intro: str = os.path.join(assets_dir, "intro.wav")
    asset_closing: str = os.path.join(assets_dir, "closing.wav")

    ##
    create_podcast(
        asset_intro_file=asset_intro,
        asset_music_segue_file=asset_segue_music,
        asset_closing_file=asset_closing,
        episode_intro_file=intro_wav,
        episode_interview_file=interview_wav,
        output_directory=output_dir,
        # output_formats=["wav", "mp3"],
    )
