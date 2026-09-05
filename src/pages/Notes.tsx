import React, { useEffect, useMemo, useRef, useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useSettings } from '../contexts/SettingsContext';
import { useLocation } from 'react-router-dom';
import { addDoc, collection, deleteDoc, deleteField, doc, onSnapshot, orderBy, query, updateDoc } from 'firebase/firestore';
import { Capacitor } from '@capacitor/core';
import { SpeechRecognition } from '@capacitor-community/speech-recognition';
import { db } from '../lib/firebase';
import { Archive, CheckSquare, LayoutGrid, List, Mic, MicOff, Palette, Pin, Plus, Search, StickyNote, Tag, Trash2, X } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

type NoteType = 'text' | 'checklist';
type ViewMode = 'grid' | 'list';
type FilterMode = 'all' | 'archived';

type ChecklistItem = {
  id: string;
  text: string;
  checked: boolean;
};

type DraftState = {
  title: string;
  content: string;
  labels: string[];
  newLabel: string;
  type: NoteType;
  checklist: ChecklistItem[];
  newItem: string;
  color: string;
  isPinned: boolean;
};

const COLORS = [
  { name: 'Default', bg: 'bg-white dark:bg-gray-800', border: 'border-gray-200 dark:border-gray-700' },
  { name: 'Red', bg: 'bg-red-50 dark:bg-red-900/20', border: 'border-red-200 dark:border-red-800' },
  { name: 'Orange', bg: 'bg-orange-50 dark:bg-orange-900/20', border: 'border-orange-200 dark:border-orange-800' },
  { name: 'Yellow', bg: 'bg-yellow-50 dark:bg-yellow-900/20', border: 'border-yellow-200 dark:border-yellow-800' },
  { name: 'Green', bg: 'bg-green-50 dark:bg-green-900/20', border: 'border-green-200 dark:border-green-800' },
  { name: 'Teal', bg: 'bg-teal-50 dark:bg-teal-900/20', border: 'border-teal-200 dark:border-teal-800' },
  { name: 'Blue', bg: 'bg-blue-50 dark:bg-blue-900/20', border: 'border-blue-200 dark:border-blue-800' },
  { name: 'Purple', bg: 'bg-purple-50 dark:bg-purple-900/20', border: 'border-purple-200 dark:border-purple-800' },
  { name: 'Pink', bg: 'bg-pink-50 dark:bg-pink-900/20', border: 'border-pink-200 dark:border-pink-800' },
];

const EMPTY_DRAFT: DraftState = {
  title: '',
  content: '',
  labels: [],
  newLabel: '',
  type: 'text',
  checklist: [],
  newItem: '',
  color: COLORS[0].bg,
  isPinned: false,
};

const parseLabels = (category?: string) => {
  if (!category || typeof category !== 'string') return [];
  return category.split(',').map((item) => item.trim()).filter(Boolean);
};

const serializeLabels = (labels: string[]) => labels.join(',').substring(0, 50);

const stripHtml = (html: string) => html.replace(/<[^>]*>/g, ' ');

const sanitizeRichHtml = (html: string) => {
  return html
    .replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, '')
    .replace(/on\w+\s*=\s*"[^"]*"/gi, '')
    .replace(/on\w+\s*=\s*'[^']*'/gi, '')
    .trim();
};

const normalizeDateString = (value: any) => {
  if (!value) return undefined;
  if (typeof value === 'string') return value;
  if (value instanceof Date) return value.toISOString();
  if (value?.toDate && typeof value.toDate === 'function') return value.toDate().toISOString();
  return undefined;
};

export const Notes: React.FC = () => {
  const { user } = useAuth();
  const { t } = useSettings();
  const location = useLocation();

  const [notes, setNotes] = useState<any[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [viewMode, setViewMode] = useState<ViewMode>('grid');
  const [filter, setFilter] = useState<FilterMode>('all');

  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editingNote, setEditingNote] = useState<any | null>(null);
  const [draft, setDraft] = useState<DraftState>(EMPTY_DRAFT);

  const [showCreateColorPicker, setShowCreateColorPicker] = useState(false);
  const [showEditColorPicker, setShowEditColorPicker] = useState(false);

  const [isRecording, setIsRecording] = useState(false);
  const recognitionRef = useRef<any>(null);
  const nativeLastPartialRef = useRef('');

  const setupSpeechRecognition = () => {
    if (recognitionRef.current) return true;

    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRecognition) return false;

    const recognition = new SpeechRecognition();
    recognition.continuous = true;
    recognition.interimResults = true;
    recognition.lang = (navigator.language || 'bn-BD');
    recognition.maxAlternatives = 1;

    recognition.onresult = (event: any) => {
      let finalTranscript = '';
      for (let i = event.resultIndex; i < event.results.length; i += 1) {
        if (event.results[i].isFinal) {
          finalTranscript += event.results[i][0].transcript;
        }
      }

      if (finalTranscript.trim()) {
        setDraft((prev) => ({
          ...prev,
          content: `${prev.content}${prev.content ? ' ' : ''}${finalTranscript.trim()}`,
        }));
      }
    };

    recognition.onerror = (event: any) => {
      console.error('Speech recognition error:', event?.error || event);
      setIsRecording(false);
    };

    recognition.onend = () => {
      setIsRecording(false);
    };

    recognitionRef.current = recognition;
    return true;
  };

  const ensureMicPermission = async () => {
    if (!navigator.mediaDevices?.getUserMedia) return;
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    stream.getTracks().forEach((track) => track.stop());
  };

  const appendVoiceText = (text: string) => {
    const normalized = text.trim();
    if (!normalized) return;
    setDraft((prev) => ({
      ...prev,
      content: `${prev.content}${prev.content ? ' ' : ''}${normalized}`,
    }));
  };

  const requestNativeSpeechPermission = async () => {
    const current = await SpeechRecognition.checkPermissions();
    if (current.speechRecognition === 'granted') return true;

    const requested = await SpeechRecognition.requestPermissions();
    return requested.speechRecognition === 'granted';
  };

  const startNativeRecording = async () => {
    const available = await SpeechRecognition.available();
    if (!available.available) throw new Error('Native speech recognition unavailable on this device.');

    const permissionGranted = await requestNativeSpeechPermission();
    if (!permissionGranted) throw new Error('Speech permission denied.');

    nativeLastPartialRef.current = '';
    await SpeechRecognition.removeAllListeners();

    await SpeechRecognition.addListener('partialResults', (data) => {
      const currentText = (data.matches || []).join(' ').trim();
      if (!currentText) return;

      const previousText = nativeLastPartialRef.current;
      let appendText = currentText;

      if (previousText && currentText.startsWith(previousText)) {
        appendText = currentText.slice(previousText.length).trim();
      }

      if (appendText) appendVoiceText(appendText);
      nativeLastPartialRef.current = currentText;
    });

    await SpeechRecognition.addListener('listeningState', ({ status }) => {
      setIsRecording(status === 'started');
      if (status === 'stopped') {
        nativeLastPartialRef.current = '';
      }
    });

    await SpeechRecognition.start({
      language: navigator.language || 'bn-BD',
      maxResults: 1,
      partialResults: true,
      popup: false,
    });

    setIsRecording(true);
  };

  const stopNativeRecording = async () => {
    await SpeechRecognition.stop();
    await SpeechRecognition.removeAllListeners();
    nativeLastPartialRef.current = '';
    setIsRecording(false);
  };

  useEffect(() => {
    if (!user) return;
    const notesRef = collection(db, 'users', user.uid, 'notes');
    const notesQuery = query(notesRef, orderBy('createdAt', 'desc'));

    const unsubscribe = onSnapshot(notesQuery, (snapshot) => {
      setNotes(snapshot.docs.map((noteDoc) => ({ id: noteDoc.id, ...noteDoc.data() })));
    });

    return () => unsubscribe();
  }, [user]);

  useEffect(() => {
    if (!Capacitor.isNativePlatform()) {
      setupSpeechRecognition();
    }

    return () => {
      if (recognitionRef.current) recognitionRef.current.stop();
      if (Capacitor.isNativePlatform()) {
        SpeechRecognition.stop().catch(() => undefined);
        SpeechRecognition.removeAllListeners().catch(() => undefined);
      }
    };
  }, []);

  useEffect(() => {
    if (location.state?.openAddModal) {
      setIsCreateOpen(true);
      window.history.replaceState({}, document.title);
    }
  }, [location]);

  const resetDraft = () => {
    setDraft(EMPTY_DRAFT);
    setShowCreateColorPicker(false);
    setShowEditColorPicker(false);
    setIsRecording(false);
  };

  const openCreate = (type: NoteType = 'text') => {
    setEditingNote(null);
    setIsCreateOpen(true);
    setDraft({ ...EMPTY_DRAFT, type });
  };

  const openEdit = (note: any) => {
    setIsCreateOpen(false);
    setEditingNote(note);
    setDraft({
      title: note.title || '',
      content: note.content || '',
      labels: parseLabels(note.category),
      newLabel: '',
      type: note.type === 'checklist' ? 'checklist' : 'text',
      checklist: Array.isArray(note.checklist) ? note.checklist : [],
      newItem: '',
      color: typeof note.color === 'string' ? note.color : COLORS[0].bg,
      isPinned: Boolean(note.isPinned),
    });
  };

  const closeCreate = () => {
    setIsCreateOpen(false);
    resetDraft();
  };

  const closeEdit = () => {
    setEditingNote(null);
    resetDraft();
  };

  const selectedColor = useMemo(
    () => COLORS.find((colorItem) => colorItem.bg === draft.color) || COLORS[0],
    [draft.color]
  );

  const isDraftEmpty = () => {
    const hasChecklist = draft.checklist.some((item) => item.text.trim());
    return !draft.title.trim() && !draft.content.trim() && !draft.newItem.trim() && !hasChecklist;
  };

  const getSanitizedChecklist = () => {
    const list = [...draft.checklist];
    if (draft.newItem.trim()) {
      list.push({ id: Date.now().toString(), text: draft.newItem.trim(), checked: false });
    }
    return list
      .filter((item) => item.text?.trim())
      .map((item) => ({ ...item, text: item.text.trim() }));
  };

  const getCreatePayload = () => {
    const checklist = draft.type === 'checklist' ? getSanitizedChecklist() : [];
    return {
      title: draft.title.trim(),
      content: draft.type === 'text' ? draft.content : '',
      type: draft.type,
      checklist,
      category: serializeLabels(draft.labels),
      color: draft.color,
      isPinned: draft.isPinned,
      isArchived: filter === 'archived',
      isVoiceNote: false,
      updatedAt: new Date().toISOString(),
      createdAt: new Date().toISOString(),
    };
  };

  const getRuleSafeUpdatePayload = (note: any, overrides: Record<string, any> = {}) => {
    const createdAt = normalizeDateString(note?.createdAt);
    const type: NoteType = note?.type === 'checklist' ? 'checklist' : 'text';

    return {
      title: typeof note?.title === 'string' ? note.title : '',
      content: type === 'text' ? (typeof note?.content === 'string' ? note.content : '') : '',
      type,
      checklist: Array.isArray(note?.checklist) ? note.checklist : [],
      color: typeof note?.color === 'string' ? note.color : COLORS[0].bg,
      category: typeof note?.category === 'string' ? note.category.substring(0, 50) : '',
      isVoiceNote: Boolean(note?.isVoiceNote),
      isPinned: Boolean(note?.isPinned),
      isArchived: Boolean(note?.isArchived),
      ...(typeof note?.voiceURL === 'string' && note.voiceURL ? { voiceURL: note.voiceURL } : {}),
      ...(createdAt ? { createdAt } : {}),
      labels: deleteField(),
      updatedAt: new Date().toISOString(),
      ...overrides,
    };
  };

  const saveDraft = async () => {
    if (!user || isDraftEmpty()) return;

    try {
      if (editingNote) {
        const checklist = draft.type === 'checklist' ? getSanitizedChecklist() : [];
        const payload = getRuleSafeUpdatePayload(editingNote, {
          title: draft.title.trim(),
          content: draft.type === 'text' ? draft.content : '',
          type: draft.type,
          checklist,
          category: serializeLabels(draft.labels),
          color: draft.color,
          isPinned: draft.isPinned,
        });

        await updateDoc(doc(db, 'users', user.uid, 'notes', editingNote.id), payload);
        closeEdit();
      } else {
        await addDoc(collection(db, 'users', user.uid, 'notes'), getCreatePayload());
        closeCreate();
      }
    } catch (error) {
      console.error('Error saving note:', error);
    }
  };

  const updateNote = async (note: any, updates: Record<string, any>) => {
    if (!user) return;
    try {
      await updateDoc(doc(db, 'users', user.uid, 'notes', note.id), getRuleSafeUpdatePayload(note, updates));
    } catch (error) {
      console.error('Error updating note:', error);
    }
  };

  const togglePin = async (note: any) => {
    await updateNote(note, { isPinned: !note.isPinned, isArchived: false });
  };

  const toggleArchive = async (note: any) => {
    await updateNote(note, { isArchived: !note.isArchived, isPinned: false });
  };

  const toggleChecklistItem = async (note: any, itemId: string) => {
    const checklist = (note.checklist || []).map((item: any) =>
      item.id === itemId ? { ...item, checked: !item.checked } : item
    );
    await updateNote(note, { checklist, type: 'checklist' });
  };

  const deleteNote = async (id: string) => {
    if (!user) return;
    try {
      await deleteDoc(doc(db, 'users', user.uid, 'notes', id));
      if (editingNote?.id === id) closeEdit();
    } catch (error) {
      console.error('Error deleting note:', error);
    }
  };

  const toggleRecording = () => {
    const run = async () => {
      if (Capacitor.isNativePlatform()) {
        try {
          if (isRecording) {
            await stopNativeRecording();
          } else {
            await startNativeRecording();
          }
        } catch (error) {
          console.error('Could not toggle native voice typing:', error);
          setIsRecording(false);
        }
        return;
      }

      const hasSupport = setupSpeechRecognition();
      if (!hasSupport || !recognitionRef.current) {
        console.error('Speech recognition is not supported in this environment.');
        return;
      }

      if (isRecording) {
        recognitionRef.current.stop();
        setIsRecording(false);
        return;
      }

      try {
        await ensureMicPermission();
        recognitionRef.current.start();
        setIsRecording(true);
      } catch (error) {
        console.error('Could not start voice typing:', error);
        setIsRecording(false);
      }
    };

    run();
  };

  const addLabel = () => {
    const value = draft.newLabel.trim();
    if (!value || draft.labels.includes(value)) return;
    setDraft((prev) => ({ ...prev, labels: [...prev.labels, value], newLabel: '' }));
  };

  const filteredNotes = useMemo(() => {
    const base = notes.filter((note) => (filter === 'all' ? !note.isArchived : note.isArchived));
    if (!searchQuery.trim()) return base;

    const needle = searchQuery.toLowerCase();
    return base.filter((note) => {
      const category = typeof note.category === 'string' ? note.category : '';
      const checklist = Array.isArray(note.checklist) ? note.checklist : [];
      const contentText = stripHtml(String(note.content || '')).toLowerCase();
      return (
        String(note.title || '').toLowerCase().includes(needle)
        || contentText.includes(needle)
        || category.toLowerCase().includes(needle)
        || checklist.some((item: any) => String(item.text || '').toLowerCase().includes(needle))
      );
    });
  }, [notes, filter, searchQuery]);

  const pinnedNotes = filteredNotes.filter((note) => note.isPinned);
  const otherNotes = filteredNotes.filter((note) => !note.isPinned);

  return (
    <div className="space-y-6 pb-20 max-w-5xl mx-auto px-4">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pt-4">
        <div>
          <h2 className="text-2xl font-bold text-gray-900 dark:text-white">{t('notes') || 'Notes'}</h2>
          <div className="flex space-x-4 mt-2">
            <button
              onClick={() => setFilter('all')}
              className={`text-sm font-bold ${filter === 'all' ? 'text-indigo-600 dark:text-indigo-400 border-b-2 border-indigo-600 dark:border-indigo-400' : 'text-gray-500 dark:text-gray-400'}`}
            >
              {t('allNotes') || 'All Notes'}
            </button>
            <button
              onClick={() => setFilter('archived')}
              className={`text-sm font-bold ${filter === 'archived' ? 'text-indigo-600 dark:text-indigo-400 border-b-2 border-indigo-600 dark:border-indigo-400' : 'text-gray-500 dark:text-gray-400'}`}
            >
              {t('archived') || 'Archived'}
            </button>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <div className="relative flex-1 md:w-64">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
            <input
              type="text"
              placeholder={t('searchPlaceholder') || 'Search notes...'}
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 dark:text-white"
            />
          </div>
          <button
            onClick={() => setViewMode(viewMode === 'grid' ? 'list' : 'grid')}
            className="p-2 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
            title={viewMode === 'grid' ? 'List View' : 'Grid View'}
          >
            {viewMode === 'grid' ? <List size={20} /> : <LayoutGrid size={20} />}
          </button>
        </div>
      </div>

      <div className="relative flex justify-center z-20">
        <div className={`w-full md:w-[640px] bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-2xl shadow-lg transition-all duration-200 overflow-visible ${isCreateOpen ? `${selectedColor.bg} ${selectedColor.border}` : ''}`}>
          {!isCreateOpen ? (
            <div onClick={() => openCreate('text')} className="px-4 py-3 flex items-center justify-between cursor-pointer text-gray-500 dark:text-gray-400 font-medium">
              <span>{t('takeANote') || 'Take a note...'}</span>
              <div className="flex gap-4">
                <CheckSquare size={20} onClick={(event) => { event.stopPropagation(); openCreate('checklist'); }} className="hover:text-gray-800 dark:hover:text-white" />
                <Palette size={20} onClick={(event) => { event.stopPropagation(); openCreate('text'); setShowCreateColorPicker(true); }} className="hover:text-gray-800 dark:hover:text-white" />
              </div>
            </div>
          ) : (
            <div className="p-4 flex flex-col gap-3">
              <div className="flex justify-between items-start gap-2">
                <input
                  type="text"
                  placeholder={t('noteTitle') || 'Title'}
                  value={draft.title}
                  onChange={(event) => setDraft((prev) => ({ ...prev, title: event.target.value }))}
                  className="w-full bg-transparent text-lg font-bold placeholder-gray-500 dark:placeholder-gray-400 focus:outline-none text-gray-900 dark:text-white"
                />
                <button onClick={() => setDraft((prev) => ({ ...prev, isPinned: !prev.isPinned }))} className={`p-2 rounded-full ${draft.isPinned ? 'text-indigo-600' : 'text-gray-400 hover:text-gray-800'} hover:bg-black/5 dark:hover:bg-white/5`}>
                  <Pin size={20} fill={draft.isPinned ? 'currentColor' : 'none'} />
                </button>
              </div>

              {draft.type === 'text' ? (
                <div className="relative">
                  <RichTextEditor
                    value={draft.content}
                    placeholder={t('notePlaceholder') || 'Take a note...'}
                    onChange={(value) => setDraft((prev) => ({ ...prev, content: value }))}
                  />
                  <div className="flex justify-end">
                    <button onClick={toggleRecording} className={`p-2 rounded-full ${isRecording ? 'text-red-500 animate-pulse bg-red-50 dark:bg-red-900/20' : 'text-gray-400 hover:text-gray-800 hover:bg-black/5 dark:hover:bg-white/5'}`}>
                      {isRecording ? <MicOff size={18} /> : <Mic size={18} />}
                    </button>
                  </div>
                </div>
              ) : (
                <div className="space-y-2">
                  {draft.checklist.map((item) => (
                    <div key={item.id} className="flex items-center space-x-3 group">
                      <button onClick={() => setDraft((prev) => ({ ...prev, checklist: prev.checklist.map((check) => (check.id === item.id ? { ...check, checked: !check.checked } : check)) }))} className={`text-gray-400 ${item.checked ? 'text-indigo-500' : ''}`}>
                        {item.checked ? <CheckSquare size={18} /> : <div className="w-[18px] h-[18px] border-2 border-gray-400 rounded-sm" />}
                      </button>
                      <input
                        type="text"
                        value={item.text}
                        onChange={(event) => setDraft((prev) => ({ ...prev, checklist: prev.checklist.map((check) => (check.id === item.id ? { ...check, text: event.target.value } : check)) }))}
                        className={`flex-1 bg-transparent focus:outline-none text-sm ${item.checked ? 'line-through text-gray-500' : 'text-gray-900 dark:text-white'}`}
                      />
                      <button onClick={() => setDraft((prev) => ({ ...prev, checklist: prev.checklist.filter((check) => check.id !== item.id) }))} className="text-gray-400 hover:text-red-500 p-1">
                        <X size={16} />
                      </button>
                    </div>
                  ))}
                  <div className="flex items-center space-x-3 text-gray-500 pt-1">
                    <Plus size={18} />
                    <input
                      type="text"
                      placeholder={t('addItem') || 'List item'}
                      value={draft.newItem}
                      onChange={(event) => setDraft((prev) => ({ ...prev, newItem: event.target.value }))}
                      onKeyDown={(event) => {
                        if (event.key === 'Enter' && draft.newItem.trim()) {
                          event.preventDefault();
                          setDraft((prev) => ({ ...prev, checklist: [...prev.checklist, { id: Date.now().toString(), text: prev.newItem.trim(), checked: false }], newItem: '' }));
                        }
                      }}
                      className="flex-1 bg-transparent focus:outline-none text-sm placeholder-gray-500"
                    />
                  </div>
                </div>
              )}

              {draft.labels.length > 0 && (
                <div className="flex flex-wrap gap-2 mt-1">
                  {draft.labels.map((label) => (
                    <span key={label} className="flex items-center gap-1 px-2 py-1 bg-black/5 dark:bg-white/10 rounded-full text-xs text-gray-700 dark:text-gray-300">
                      {label}
                      <X size={12} className="cursor-pointer hover:text-red-500" onClick={() => setDraft((prev) => ({ ...prev, labels: prev.labels.filter((item) => item !== label) }))} />
                    </span>
                  ))}
                </div>
              )}

              <div className="flex flex-col sm:flex-row gap-3 sm:justify-between sm:items-center mt-2 relative">
                <div className="flex flex-wrap items-center gap-1">
                  <div className="relative">
                    <button onClick={() => setShowCreateColorPicker((prev) => !prev)} className="p-2 rounded-full text-gray-500 hover:bg-black/5 dark:hover:bg-white/10">
                      <Palette size={18} />
                    </button>
                    {showCreateColorPicker && (
                      <div className="absolute top-10 left-0 bg-white dark:bg-gray-800 p-2 rounded-xl shadow-xl flex gap-2 border border-gray-200 dark:border-gray-700 z-[120]">
                        {COLORS.map((colorItem) => (
                          <div key={colorItem.name} onClick={() => { setDraft((prev) => ({ ...prev, color: colorItem.bg })); setShowCreateColorPicker(false); }} className={`w-7 h-7 rounded-full cursor-pointer border ${colorItem.bg} ${colorItem.border} ${draft.color === colorItem.bg ? 'ring-2 ring-indigo-500 ring-offset-2' : ''}`} />
                        ))}
                      </div>
                    )}
                  </div>

                  <button onClick={() => setDraft((prev) => ({ ...prev, type: prev.type === 'text' ? 'checklist' : 'text' }))} className="p-2 rounded-full text-gray-500 hover:bg-black/5 dark:hover:bg-white/10">
                    <CheckSquare size={18} />
                  </button>

                  <div className="flex items-center gap-1">
                    <button className="p-2 rounded-full text-gray-500 hover:bg-black/5 dark:hover:bg-white/10">
                      <Tag size={18} />
                    </button>
                    <input
                      type="text"
                      placeholder="Label"
                      value={draft.newLabel}
                      onChange={(event) => setDraft((prev) => ({ ...prev, newLabel: event.target.value }))}
                      onKeyDown={(event) => {
                        if (event.key === 'Enter') {
                          event.preventDefault();
                          addLabel();
                        }
                      }}
                      className="w-24 bg-transparent border-b border-gray-300 dark:border-gray-600 focus:outline-none text-xs text-gray-800 dark:text-white"
                    />
                    <button onClick={addLabel} className="px-2 py-1 text-xs rounded-md bg-black/5 dark:bg-white/10 text-gray-700 dark:text-gray-200 hover:bg-black/10 dark:hover:bg-white/20">
                      +
                    </button>
                  </div>
                </div>

                <div className="flex items-center gap-2 self-end sm:self-auto">
                  <button onClick={closeCreate} className="px-4 py-1.5 text-sm font-semibold text-gray-600 dark:text-gray-300 hover:bg-black/5 dark:hover:bg-white/10 rounded-lg">
                    {t('cancel') || 'Cancel'}
                  </button>
                  <button onClick={saveDraft} className="px-5 py-1.5 font-bold text-sm bg-black dark:bg-white text-white dark:text-black rounded-lg hover:opacity-80">
                    {t('save') || 'Save'}
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      <div className="space-y-8 mt-8">
        {pinnedNotes.length > 0 && (
          <div>
            <h3 className="text-xs font-bold text-gray-500 uppercase tracking-widest mb-4 flex items-center">
              <Pin size={14} className="mr-2" /> {t('pinned') || 'Pinned'}
            </h3>
            <NotesGrid
              notes={pinnedNotes}
              viewMode={viewMode}
              onEdit={openEdit}
              onDelete={deleteNote}
              onPin={togglePin}
              onArchive={toggleArchive}
              onToggleCheck={toggleChecklistItem}
            />
          </div>
        )}

        <div>
          {pinnedNotes.length > 0 && otherNotes.length > 0 && (
            <h3 className="text-xs font-bold text-gray-500 uppercase tracking-widest mb-4">{t('others') || 'Others'}</h3>
          )}
          <NotesGrid
            notes={otherNotes}
            viewMode={viewMode}
            onEdit={openEdit}
            onDelete={deleteNote}
            onPin={togglePin}
            onArchive={toggleArchive}
            onToggleCheck={toggleChecklistItem}
          />
        </div>

        {filteredNotes.length === 0 && !isCreateOpen && !editingNote && (
          <div className="text-center py-20 flex flex-col items-center opacity-50">
            <StickyNote size={64} className="mb-4 text-gray-400" />
            <h3 className="text-xl font-bold text-gray-600 dark:text-gray-300">{searchQuery ? 'No matching notes' : (t('noNotesFound') || 'Notes you add appear here')}</h3>
          </div>
        )}
      </div>

      <AnimatePresence>
        {editingNote && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 z-50">
            <button
              className="absolute inset-0 bg-black/40"
              onClick={closeEdit}
              aria-label="Close editor"
            />
            <div className="relative z-10 h-full w-full overflow-y-auto p-4 sm:p-6 flex items-start sm:items-center justify-center">
              <motion.div initial={{ y: 20, opacity: 0 }} animate={{ y: 0, opacity: 1 }} exit={{ y: 20, opacity: 0 }} className={`w-full max-w-2xl rounded-2xl border shadow-xl overflow-visible ${selectedColor.bg} ${selectedColor.border}`}>
                <div className="p-4 sm:p-5 flex flex-col gap-3">
                  <div className="flex justify-between items-start gap-2">
                    <input
                      type="text"
                      placeholder={t('noteTitle') || 'Title'}
                      value={draft.title}
                      onChange={(event) => setDraft((prev) => ({ ...prev, title: event.target.value }))}
                      className="w-full bg-transparent text-lg font-bold placeholder-gray-500 dark:placeholder-gray-400 focus:outline-none text-gray-900 dark:text-white"
                    />
                    <button onClick={() => setDraft((prev) => ({ ...prev, isPinned: !prev.isPinned }))} className={`p-2 rounded-full ${draft.isPinned ? 'text-indigo-600' : 'text-gray-400 hover:text-gray-800'} hover:bg-black/5 dark:hover:bg-white/5`}>
                      <Pin size={20} fill={draft.isPinned ? 'currentColor' : 'none'} />
                    </button>
                  </div>

                  {draft.type === 'text' ? (
                    <div className="space-y-2">
                      <RichTextEditor
                        value={draft.content}
                        placeholder={t('notePlaceholder') || 'Take a note...'}
                        onChange={(value) => setDraft((prev) => ({ ...prev, content: value }))}
                      />
                      <div className="flex justify-end">
                        <button onClick={toggleRecording} className={`p-2 rounded-full ${isRecording ? 'text-red-500 animate-pulse bg-red-50 dark:bg-red-900/20' : 'text-gray-400 hover:text-gray-800 hover:bg-black/5 dark:hover:bg-white/5'}`}>
                          {isRecording ? <MicOff size={18} /> : <Mic size={18} />}
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div className="space-y-2">
                      {draft.checklist.map((item) => (
                        <div key={item.id} className="flex items-center space-x-3 group">
                          <button onClick={() => setDraft((prev) => ({ ...prev, checklist: prev.checklist.map((check) => (check.id === item.id ? { ...check, checked: !check.checked } : check)) }))} className={`text-gray-400 ${item.checked ? 'text-indigo-500' : ''}`}>
                            {item.checked ? <CheckSquare size={18} /> : <div className="w-[18px] h-[18px] border-2 border-gray-400 rounded-sm" />}
                          </button>
                          <input
                            type="text"
                            value={item.text}
                            onChange={(event) => setDraft((prev) => ({ ...prev, checklist: prev.checklist.map((check) => (check.id === item.id ? { ...check, text: event.target.value } : check)) }))}
                            className={`flex-1 bg-transparent focus:outline-none text-sm ${item.checked ? 'line-through text-gray-500' : 'text-gray-900 dark:text-white'}`}
                          />
                          <button onClick={() => setDraft((prev) => ({ ...prev, checklist: prev.checklist.filter((check) => check.id !== item.id) }))} className="text-gray-400 hover:text-red-500 p-1">
                            <X size={16} />
                          </button>
                        </div>
                      ))}
                      <div className="flex items-center space-x-3 text-gray-500 pt-1">
                        <Plus size={18} />
                        <input
                          type="text"
                          placeholder={t('addItem') || 'List item'}
                          value={draft.newItem}
                          onChange={(event) => setDraft((prev) => ({ ...prev, newItem: event.target.value }))}
                          onKeyDown={(event) => {
                            if (event.key === 'Enter' && draft.newItem.trim()) {
                              event.preventDefault();
                              setDraft((prev) => ({ ...prev, checklist: [...prev.checklist, { id: Date.now().toString(), text: prev.newItem.trim(), checked: false }], newItem: '' }));
                            }
                          }}
                          className="flex-1 bg-transparent focus:outline-none text-sm placeholder-gray-500"
                        />
                      </div>
                    </div>
                  )}

                  {draft.labels.length > 0 && (
                    <div className="flex flex-wrap gap-2 mt-1">
                      {draft.labels.map((label) => (
                        <span key={label} className="flex items-center gap-1 px-2 py-1 bg-black/5 dark:bg-white/10 rounded-full text-xs text-gray-700 dark:text-gray-300">
                          {label}
                          <X size={12} className="cursor-pointer hover:text-red-500" onClick={() => setDraft((prev) => ({ ...prev, labels: prev.labels.filter((item) => item !== label) }))} />
                        </span>
                      ))}
                    </div>
                  )}

                  <div className="flex flex-col sm:flex-row gap-3 sm:justify-between sm:items-center mt-2 relative">
                    <div className="flex flex-wrap items-center gap-1">
                      <div className="relative">
                        <button onClick={() => setShowEditColorPicker((prev) => !prev)} className="p-2 rounded-full text-gray-500 hover:bg-black/5 dark:hover:bg-white/10">
                          <Palette size={18} />
                        </button>
                        {showEditColorPicker && (
                          <div className="absolute top-10 left-0 bg-white dark:bg-gray-800 p-2 rounded-xl shadow-xl flex gap-2 border border-gray-200 dark:border-gray-700 z-[130]">
                            {COLORS.map((colorItem) => (
                              <div key={colorItem.name} onClick={() => { setDraft((prev) => ({ ...prev, color: colorItem.bg })); setShowEditColorPicker(false); }} className={`w-7 h-7 rounded-full cursor-pointer border ${colorItem.bg} ${colorItem.border} ${draft.color === colorItem.bg ? 'ring-2 ring-indigo-500 ring-offset-2' : ''}`} />
                            ))}
                          </div>
                        )}
                      </div>

                      <button onClick={() => setDraft((prev) => ({ ...prev, type: prev.type === 'text' ? 'checklist' : 'text' }))} className="p-2 rounded-full text-gray-500 hover:bg-black/5 dark:hover:bg-white/10">
                        <CheckSquare size={18} />
                      </button>

                      <button onClick={() => deleteNote(editingNote.id)} className="p-2 rounded-full text-gray-500 hover:text-red-600 hover:bg-red-100 dark:hover:bg-red-900/30">
                        <Trash2 size={18} />
                      </button>

                      <button className="p-2 rounded-full text-gray-500 hover:bg-black/5 dark:hover:bg-white/10">
                        <Tag size={18} />
                      </button>

                      <input
                        type="text"
                        placeholder="Label"
                        value={draft.newLabel}
                        onChange={(event) => setDraft((prev) => ({ ...prev, newLabel: event.target.value }))}
                        onKeyDown={(event) => {
                          if (event.key === 'Enter') {
                            event.preventDefault();
                            addLabel();
                          }
                        }}
                        className="w-24 bg-transparent border-b border-gray-300 dark:border-gray-600 focus:outline-none text-xs text-gray-800 dark:text-white"
                      />
                      <button onClick={addLabel} className="px-2 py-1 text-xs rounded-md bg-black/5 dark:bg-white/10 text-gray-700 dark:text-gray-200 hover:bg-black/10 dark:hover:bg-white/20">
                        +
                      </button>
                    </div>

                    <div className="flex items-center gap-2 self-end sm:self-auto">
                      <button onClick={closeEdit} className="px-4 py-1.5 text-sm font-semibold text-gray-600 dark:text-gray-300 hover:bg-black/5 dark:hover:bg-white/10 rounded-lg">
                        {t('close') || 'Close'}
                      </button>
                      <button onClick={saveDraft} className="px-5 py-1.5 font-bold text-sm bg-black dark:bg-white text-white dark:text-black rounded-lg hover:opacity-80">
                        {t('update') || 'Update'}
                      </button>
                    </div>
                  </div>
                </div>
              </motion.div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

type NotesGridProps = {
  notes: any[];
  viewMode: ViewMode;
  onEdit: (note: any) => void;
  onDelete: (id: string) => void;
  onPin: (note: any) => void;
  onArchive: (note: any) => void;
  onToggleCheck: (note: any, itemId: string) => void;
};

type RichTextEditorProps = {
  value: string;
  placeholder: string;
  onChange: (value: string) => void;
};

const RichTextEditor: React.FC<RichTextEditorProps> = ({ value, placeholder, onChange }) => {
  const editorRef = useRef<HTMLDivElement>(null);
  const textColorRef = useRef<HTMLInputElement>(null);
  const highlightColorRef = useRef<HTMLInputElement>(null);
  const savedRangeRef = useRef<Range | null>(null);

  useEffect(() => {
    if (!editorRef.current) return;
    if (editorRef.current.innerHTML !== value) {
      editorRef.current.innerHTML = value || '';
    }
  }, [value]);

  const focusEditor = () => {
    editorRef.current?.focus();
  };

  const saveSelection = () => {
    const selection = window.getSelection();
    if (!selection || selection.rangeCount === 0 || !editorRef.current) return;

    const range = selection.getRangeAt(0);
    if (editorRef.current.contains(range.commonAncestorContainer)) {
      savedRangeRef.current = range.cloneRange();
    }
  };

  const restoreSelection = () => {
    const selection = window.getSelection();
    if (!selection || !savedRangeRef.current) return;
    selection.removeAllRanges();
    selection.addRange(savedRangeRef.current);
  };

  const applyCommand = (command: string, valueArg?: string) => {
    restoreSelection();
    focusEditor();
    const applied = document.execCommand(command, false, valueArg);

    if (!applied && command === 'insertOrderedList') {
      document.execCommand('insertText', false, '1. ');
    }
    if (!applied && command === 'insertUnorderedList') {
      document.execCommand('insertText', false, '• ');
    }

    saveSelection();
    onChange(editorRef.current?.innerHTML || '');
  };

  const getCurrentLineTextBeforeCursor = () => {
    const selection = window.getSelection();
    if (!selection || selection.rangeCount === 0 || !editorRef.current) return '';

    const range = selection.getRangeAt(0).cloneRange();
    range.selectNodeContents(editorRef.current);
    range.setEnd(selection.anchorNode as Node, selection.anchorOffset);

    const beforeCursor = range.toString();
    const lines = beforeCursor.split(/\r?\n/);
    return lines[lines.length - 1].trim();
  };

  const handleAutoList = (event: React.KeyboardEvent<HTMLDivElement>) => {
    if (event.key !== ' ') return;

    const line = getCurrentLineTextBeforeCursor();
    if (!line) return;

    const isOrdered = /^(?:\d+|[০-৯]+)[\.\u0964\/\)]$/.test(line);
    const isBullet = /^[-*•+]+$/.test(line);

    if (!isOrdered && !isBullet) return;

    event.preventDefault();
    const selection = window.getSelection();
    if (!selection || selection.rangeCount === 0) return;

    const range = selection.getRangeAt(0);
    try {
      if (range.startContainer.nodeType === Node.TEXT_NODE) {
        const textNode = range.startContainer as Text;
        const deleteLength = Math.min(line.length, range.startOffset);
        range.setStart(textNode, range.startOffset - deleteLength);
        range.deleteContents();
      }
    } catch {
      // best effort deletion only
    }

    applyCommand(isOrdered ? 'insertOrderedList' : 'insertUnorderedList');
  };

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap items-center gap-1 p-2 rounded-lg border border-gray-200 dark:border-gray-700 bg-white/60 dark:bg-gray-900/20">
        <button type="button" onMouseDown={(event) => event.preventDefault()} onTouchStart={(event) => event.preventDefault()} onClick={() => applyCommand('bold')} className="px-2 py-1 text-xs rounded-md hover:bg-black/5 dark:hover:bg-white/10 font-bold">B</button>
        <button type="button" onMouseDown={(event) => event.preventDefault()} onTouchStart={(event) => event.preventDefault()} onClick={() => applyCommand('italic')} className="px-2 py-1 text-xs rounded-md hover:bg-black/5 dark:hover:bg-white/10 italic">I</button>
        <button type="button" onMouseDown={(event) => event.preventDefault()} onTouchStart={(event) => event.preventDefault()} onClick={() => applyCommand('underline')} className="px-2 py-1 text-xs rounded-md hover:bg-black/5 dark:hover:bg-white/10 underline">U</button>
        <button type="button" onMouseDown={(event) => event.preventDefault()} onTouchStart={(event) => event.preventDefault()} onClick={() => applyCommand('strikeThrough')} className="px-2 py-1 text-xs rounded-md hover:bg-black/5 dark:hover:bg-white/10 line-through">S</button>
        <button type="button" onMouseDown={(event) => event.preventDefault()} onTouchStart={(event) => event.preventDefault()} onClick={() => applyCommand('insertOrderedList')} className="px-2 py-1 text-xs rounded-md hover:bg-black/5 dark:hover:bg-white/10">1.</button>
        <button type="button" onMouseDown={(event) => event.preventDefault()} onTouchStart={(event) => event.preventDefault()} onClick={() => applyCommand('insertUnorderedList')} className="px-2 py-1 text-xs rounded-md hover:bg-black/5 dark:hover:bg-white/10">•</button>
        <button type="button" onMouseDown={(event) => event.preventDefault()} onTouchStart={(event) => event.preventDefault()} onClick={() => textColorRef.current?.click()} className="px-2 py-1 text-xs rounded-md hover:bg-black/5 dark:hover:bg-white/10">Color</button>
        <button type="button" onMouseDown={(event) => event.preventDefault()} onTouchStart={(event) => event.preventDefault()} onClick={() => highlightColorRef.current?.click()} className="px-2 py-1 text-xs rounded-md hover:bg-black/5 dark:hover:bg-white/10">Highlight</button>

        <input
          ref={textColorRef}
          type="color"
          className="sr-only"
          onChange={(event) => applyCommand('foreColor', event.target.value)}
        />
        <input
          ref={highlightColorRef}
          type="color"
          className="sr-only"
          onChange={(event) => applyCommand('hiliteColor', event.target.value)}
        />
      </div>

      <div
        ref={editorRef}
        contentEditable
        suppressContentEditableWarning
        data-placeholder={placeholder}
        spellCheck
        autoCorrect="on"
        autoCapitalize="sentences"
        onFocus={saveSelection}
        onClick={saveSelection}
        onKeyUp={saveSelection}
        onTouchEnd={saveSelection}
        onInput={(event) => onChange((event.currentTarget as HTMLDivElement).innerHTML)}
        onKeyDown={handleAutoList}
        className="w-full min-h-[120px] max-h-[45vh] overflow-y-auto bg-transparent text-sm text-gray-900 dark:text-white leading-relaxed focus:outline-none whitespace-pre-wrap break-words select-text cursor-text touch-auto [&_ol]:list-decimal [&_ol]:pl-5 [&_ul]:list-disc [&_ul]:pl-5 [&_li]:my-1 [&:empty:before]:content-[attr(data-placeholder)] [&:empty:before]:text-gray-500 dark:[&:empty:before]:text-gray-400"
      />
    </div>
  );
};

const NotesGrid: React.FC<NotesGridProps> = ({ notes, viewMode, onEdit, onDelete, onPin, onArchive, onToggleCheck }) => {
  const gridClass = viewMode === 'grid' ? 'columns-1 md:columns-2 lg:columns-3 gap-4 space-y-4' : 'flex flex-col space-y-4';

  return (
    <div className={gridClass}>
      {notes.map((note) => (
        <div key={note.id} className="break-inside-avoid">
          <NoteCard
            note={note}
            onEdit={onEdit}
            onDelete={onDelete}
            onPin={onPin}
            onArchive={onArchive}
            onToggleCheck={onToggleCheck}
          />
        </div>
      ))}
    </div>
  );
};

type NoteCardProps = {
  note: any;
  onEdit: (note: any) => void;
  onDelete: (id: string) => void;
  onPin: (note: any) => void;
  onArchive: (note: any) => void;
  onToggleCheck: (note: any, itemId: string) => void;
};

const NoteCard: React.FC<NoteCardProps> = ({ note, onEdit, onDelete, onPin, onArchive, onToggleCheck }) => {
  const { t } = useSettings();
  const color = COLORS.find((item) => item.bg === note.color) || COLORS[0];
  const labels = parseLabels(note.category);

  return (
    <motion.button
      type="button"
      layout
      initial={{ opacity: 0, scale: 0.96 }}
      animate={{ opacity: 1, scale: 1 }}
      exit={{ opacity: 0, scale: 0.96 }}
      onClick={() => onEdit(note)}
      className={`${color.bg} ${color.border} w-full p-5 rounded-2xl border shadow-sm hover:shadow-md transition-all group relative text-left flex flex-col`}
    >
      <div className="absolute top-3 right-3 opacity-100 md:opacity-0 md:group-hover:opacity-100 transition-opacity z-10">
        <button type="button" onClick={(event) => { event.stopPropagation(); onPin(note); }} className={`p-1.5 rounded-full bg-white/50 dark:bg-gray-800/50 hover:bg-black/10 dark:hover:bg-white/20 ${note.isPinned ? 'text-indigo-600' : 'text-gray-600 dark:text-gray-300'}`}>
          <Pin size={16} fill={note.isPinned ? 'currentColor' : 'none'} />
        </button>
      </div>

      {note.title && <h3 className="font-bold text-lg text-gray-900 dark:text-white leading-tight mb-3 pr-8 break-words">{note.title}</h3>}

      <div className="flex-1 overflow-hidden">
        {note.type === 'checklist' ? (
          <div className="space-y-2 mb-4">
            {(note.checklist || []).slice(0, 6).map((item: any) => (
              <div key={item.id} className="flex items-start space-x-2">
                <button
                  type="button"
                  onClick={(event) => {
                    event.stopPropagation();
                    onToggleCheck(note, item.id);
                  }}
                  className={`mt-0.5 ${item.checked ? 'text-indigo-600 dark:text-indigo-400' : 'text-gray-400'}`}
                >
                  {item.checked ? <CheckSquare size={16} /> : <div className="w-4 h-4 border border-gray-400 rounded-sm" />}
                </button>
                <span className={`text-sm leading-snug break-words ${item.checked ? 'line-through text-gray-500' : 'text-gray-800 dark:text-gray-200'}`}>{item.text}</span>
              </div>
            ))}
          </div>
        ) : (
          note.content && (
            <div
              className="text-gray-700 dark:text-gray-300 text-sm line-clamp-12 mb-4 leading-relaxed break-words prose prose-sm max-w-none dark:prose-invert"
              dangerouslySetInnerHTML={{ __html: sanitizeRichHtml(note.content) }}
            />
          )
        )}
      </div>

      {labels.length > 0 && (
        <div className="flex flex-wrap gap-1.5 mb-3 mt-auto">
          {labels.map((label) => (
            <span key={label} className="px-2 py-0.5 bg-black/5 dark:bg-white/10 text-gray-600 dark:text-gray-300 text-[10px] rounded-full font-bold truncate max-w-full">
              {label}
            </span>
          ))}
        </div>
      )}

      <div className="flex justify-between items-center pt-3 border-t border-black/5 dark:border-white/10 mt-auto opacity-100 md:opacity-0 md:group-hover:opacity-100 transition-opacity">
        <button
          type="button"
          onClick={(event) => {
            event.stopPropagation();
            onArchive(note);
          }}
          className={`p-1.5 rounded-full hover:bg-black/5 dark:hover:bg-white/10 ${note.isArchived ? 'text-indigo-600' : 'text-gray-500'}`}
          title={t('archived') || 'Archive'}
        >
          <Archive size={16} />
        </button>

        <button
          type="button"
          onClick={(event) => {
            event.stopPropagation();
            onDelete(note.id);
          }}
          className="p-1.5 rounded-full hover:bg-red-100 hover:text-red-600 dark:hover:bg-red-900/30 text-gray-500 transition-colors"
          title={t('delete') || 'Delete'}
        >
          <Trash2 size={16} />
        </button>
      </div>
    </motion.button>
  );
};
