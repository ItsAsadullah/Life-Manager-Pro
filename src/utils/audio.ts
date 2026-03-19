let sharedAudioContext: AudioContext | null = null;

export const playTick = () => {
  try {
    const AudioContextClass = window.AudioContext || (window as any).webkitAudioContext;
    if (AudioContextClass) {
      if (!sharedAudioContext) {
        sharedAudioContext = new AudioContextClass();
      }
      
      // Resume the AudioContext if it's suspended (e.g. browser autoplay policy)
      if (sharedAudioContext.state === 'suspended') {
        sharedAudioContext.resume();
      }

      const ctx = sharedAudioContext;
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      
      osc.connect(gain);
      gain.connect(ctx.destination);
      
      osc.type = 'sine';
      osc.frequency.setValueAtTime(800, ctx.currentTime);
      gain.gain.setValueAtTime(0.1, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.05);
      
      osc.start(ctx.currentTime);
      osc.stop(ctx.currentTime + 0.05);
    }
  } catch (e) {
    // ignore audio errors
    console.warn("Audio error:", e);
  }
};
