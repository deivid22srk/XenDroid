/**
 ******************************************************************************
 * Xenia : Xbox 360 Emulator Research Project                                 *
 ******************************************************************************
 * Copyright 2020 Ben Vanik. All rights reserved.                             *
 * Released under the BSD license - see LICENSE in the root for more details. *
 ******************************************************************************
 */
#ifndef XENDROID_XE_AAUDIO_AUDIO_DRIVER_H
#define XENDROID_XE_AAUDIO_AUDIO_DRIVER_H

#include <atomic>
#include <condition_variable>
#include <mutex>
#include <queue>
#include <stack>
#include <thread>

#include <aaudio/AAudio.h>

#include "xenia/apu/audio_driver.h"
#include "xenia/base/threading.h"

namespace xe {
namespace apu {
namespace aaudio {

class AAudioAudioDriver : public AudioDriver {
 public:
  AAudioAudioDriver(Memory* memory, xe::threading::Semaphore* semaphore,
                    uint32_t frequency = AudioDriver::kFrameFrequencyDefault,
                    uint32_t channels = AudioDriver::kFrameChannelsDefault,
                    bool need_format_conversion = true);
  ~AAudioAudioDriver() override;

  bool Initialize();
    void Pause() override;
    void Resume() override;
    void SetVolume(float volume) override;
  void SubmitFrame(float* frame) override;
  void Shutdown();

 protected:
  static aaudio_data_callback_result_t AudioCallback(
      AAudioStream* stream,
      void* userdata,
      void* audioData,
      int32_t numFrames);

  // Callback thread only. Entry point for the no-format-conversion path.
  aaudio_data_callback_result_t XmpAudioCallback(void* audioData,
                                                 int32_t numFrames);

  static void AudioErrorCallback(
      AAudioStream* stream,
      void* userdata,
      aaudio_result_t error);

  // Callback thread only.
  void ConcealGap(float* output, int32_t out_samples, int32_t copy_samples);
  void ApplyFadeIn();
  // XMP/no-conversion path: interleaved little-endian frames at the file's
  // channel count are copied (or folded, for 5.1 sources) into last_block_.
  // Callback thread only.
  void CopyToLastBlock(float* out, const float* src, int32_t frames);
  // AAudio has no stream volume control, so this is done in software.
  // Callback thread only.
  void ApplyGainAndClamp();

  // (Re)opens the stream on the current default device; caller holds stream_mutex_.
  bool BuildStream();
  // Rebuilds the stream on the current default device; true once a stream is
  // live (or we're shutting down). Runs only on recovery_thread_.
  bool RestartStream();
  // Performs rebuild requests from the error callback, retrying on failure.
  void RecoveryThreadMain();

  xe::threading::Semaphore* semaphore_ = nullptr;

  AAudioStreamBuilder* builder_ = nullptr;
  AAudioStream* stream_ = nullptr;
  bool stream_initialized_ = false;
  // Serializes stream lifecycle. The data callback uses its `stream` argument,
  // not stream_, so it never takes this.
  std::mutex stream_mutex_ = {};

  // Output-device-change recovery: the error callback flags a request and
  // recovery_thread_ does the close+reopen (AAudio forbids closing from the
  // callback thread). Started in Initialize, joined in Shutdown.
  std::thread recovery_thread_ = {};
  std::mutex recovery_mutex_ = {};
  std::condition_variable recovery_cv_ = {};
  bool restart_requested_ = false;
  bool recovery_quit_ = false;
  std::atomic<bool> shutting_down_{false};

  static constexpr uint32_t host_frame_channels_ = 2;
  static constexpr uint32_t channel_samples_ = 256;
  static constexpr uint32_t block_samples_ = AudioDriver::kFrameSamplesMax;

  // Source stream configuration. The main game render path submits guest
  // 5.1 big-endian blocks (frequency 48000, channels 6, format conversion
  // enabled); the XMP media player submits host little-endian interleaved
  // floats at the file's own rate and channel count (conversion disabled).
  uint32_t frame_frequency_ = AudioDriver::kFrameFrequencyDefault;
  uint32_t frame_channels_ = AudioDriver::kFrameChannelsDefault;
  bool need_format_conversion_ = true;
  // Frames of frame_channels_ samples in each queued block: 256 for 5.1
  // sources, 768 for stereo, 1536 for mono. Always block_samples_ /
  // frame_channels_.
  uint32_t source_block_frames_ = channel_samples_;
  // Host-stereo frames produced per block; equals source_block_frames_.
  uint32_t output_block_frames_ = channel_samples_;
  uint32_t host_block_samples_ = host_frame_channels_ * channel_samples_;

  std::queue<float*> frames_queued_ = {};
  std::stack<float*> frames_unused_ = {};
  std::mutex frames_mutex_ = {};

  // The no-conversion path drains a queued block across several callbacks, so
  // it keeps a cursor instead of popping one block per callback. Callback
  // thread only.
  float* drain_block_ = nullptr;
  int32_t drain_remaining_ = 0;

  // Underrun concealment: silence would put a step at both edges of every
  // gap, a ~187Hz click train at a 5.3ms block. Callback thread only.
  // Sized for the largest output block: 1536 mono-source frames folded to
  // stereo (3072 floats).
  float last_block_[2 * block_samples_] = {};
  bool last_block_valid_ = false;
  uint32_t gap_blocks_ = 0;

  bool fade_in_pending_ = false;

  // Written by the realtime callback, drained by recovery_thread_: relaxed
  // atomics only, nothing that could block the callback.
  std::atomic<uint64_t> stat_callbacks_{0};
  std::atomic<uint64_t> stat_gaps_{0};
  std::atomic<uint64_t> stat_queue_depth_sum_{0};
  std::atomic<uint32_t> stat_queue_depth_max_{0};
  // A block size we did not ask for silently changes the drain rate.
  std::atomic<int32_t> stat_unexpected_frames_{0};
  // Samples the downmix pushed past full scale.
  std::atomic<uint64_t> stat_clipped_{0};
  void LogAndResetStats();

  // Per-driver volume (XMP): written by other threads, read by the callback.
  std::atomic<float> driver_volume_{1.0f};
};

}  // namespace aaudio
}  // namespace apu
}  // namespace xe

#endif //xendroid_XE_AAUDIO_AUDIO_DRIVER_H
