import React, { useEffect, useState, useRef } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useSettings } from '../contexts/SettingsContext';
import { useLocation } from 'react-router-dom';
import { collection, query, onSnapshot, orderBy, addDoc, updateDoc, deleteDoc, doc } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { Mic, MicOff, Plus, Save, X, CheckSquare, Type, Palette, Pin, Trash2, Archive, GripVertical, StickyNote, Search, LayoutGrid, List, Tag } from 'lucide-react';
import { format } from 'date-fns';
import { motion, AnimatePresence } from 'motion/react';

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

export const Notes: React.FC = () => {
  const { user } = useAuth();
  const { t } = useSettings();
  const location = useLocation();
  
  const [notes, setNotes] = useState<any[]>([]);
  const [isAdding, setIsAdding] = useState(false);
  const [editingNote, setEditingNote] = useState<any>(null);
  
  const [searchQuery, setSearchQuery] = useState('');
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');

  // Form states
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [labels, setLabels] = useState<string[]>([]);
  const [newLabel, setNewLabel] = useState('');
  const [newItemText, setNewItemText] = useState('');
  const [noteType, setNoteType] = useState<'text' | 'checklist'>('text');
  const [checklistItems, setChecklistItems] = useState<{ id: string; text: string; checked: boolean }[]>([]);
  const [selectedColor, setSelectedColor] = useState(COLORS[0]);
  const [isPinned, setIsPinned] = useState(false);
  const [isRecording, setIsRecording] = useState(false);
  const [showColorPicker, setShowColorPicker] = useState(false);
  const [filter, setFilter] = useState<'all' | 'archived'>('all');

  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (location.state?.openAddModal) {
      setIsAdding(true);
      window.history.replaceState({}, document.title);
    }
  }, [location]);

  // Click outside to collapse/save note creator
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        if (isAdding && !editingNote && (title.trim() || content.trim() || checklistItems.length > 0 || newItemText.trim())) {
          handleSave();
        } else {
          resetForm();
        }
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isAdding, title, content, checklistItems, newItemText, editingNote]);

  const resetForm = () => {
    setTitle('');
    setContent('');
    setLabels([]);
    setNewLabel('');
    setNewItemText('');
    setNoteType('text');
    setChecklistItems([]);
    setSelectedColor(COLORS[0]);
    setIsPinned(false);
    setEditingNote(null);
    setIsAdding(false);
    setShowColorPicker(false);
  };

  const startEditing = (note: any) => {
    setEditingNote(note);
    setTitle(note.title);
    setContent(note.content || '');
    setLabels(note.category ? note.category.split(',').map((s: string) => s.trim()).filter(Boolean) : []);
    setNoteType(note.type || 'text');
    setChecklistItems(note.checklist || []);
    setSelectedColor(COLORS.find(c => c.bg === note.color) || COLORS[0]);
    setIsPinned(note.isPinned || false);
    setIsAdding(true);
  };

  const recognitionRef = useRef<any>(null);

  useEffect(() => {
    if (!user) return;
    const notesRef = collection(db, 'users', user.uid, 'notes');
    const q = query(notesRef, orderBy('createdAt', 'desc'));

    const unsubscribe = onSnapshot(q, (snapshot) => {
      setNotes(snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() })));
    });

    if ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window) {
      const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
      recognitionRef.current = new SpeechRecognition();
      recognitionRef.current.continuous = true;
      recognitionRef.current.interimResults = true;

      recognitionRef.current.onresult = (event: any) => {
        let interimTranscript = '';
        let finalTranscript = '';

        for (let i = event.resultIndex; i < event.results.length; ++i) {
          if (event.results[i].isFinal) {
            finalTranscript += event.results[i][0].transcript;
          } else {
            interimTranscript += event.results[i][0].transcript;
          }
        }

        if (finalTranscript) {
          setContent(prev => prev + (prev ? ' ' : '') + finalTranscript);
        }
      };

      recognitionRef.current.onerror = (event: any) => {
        console.error('Speech recognition error', event.error);
        setIsRecording(false);
      };
    }

    return () => {
      unsubscribe();
      if (recognitionRef.current) {
        recognitionRef.current.stop();
      }
    };
  }, [user]);

  const toggleRecording = () => {
    if (isRecording) {
      recognitionRef.current?.stop();
      setIsRecording(false);
    } else {
      setContent('');
      recognitionRef.current?.start();
      setIsRecording(true);
    }
  };

  const handleSave = async () => {
    if (!user) return;
    if (!title.trim() && !content.trim() && checklistItems.length === 0 && !newItemText.trim()) return;

    try {
      const finalChecklist = [...checklistItems];
      if (newItemText.trim() && noteType === 'checklist') {
        finalChecklist.push({ id: Date.now().toString(), text: newItemText.trim(), checked: false });
      }

      const noteData = {
        title,
        content: noteType === 'text' ? content : '',
        type: noteType,
        checklist: noteType === 'checklist' ? finalChecklist : [],
        category: labels.join(',').substring(0, 50),
        color: selectedColor.bg,
        isPinned,
        isArchived: filter === 'archived',
        isVoiceNote: false,
        updatedAt: new Date().toISOString()
      };

      if (editingNote) {
        await updateDoc(doc(db, 'users', user.uid, 'notes', editingNote.id), noteData);
      } else {
        await addDoc(collection(db, 'users', user.uid, 'notes'), {
          ...noteData,
          createdAt: new Date().toISOString()
        });
      }
      resetForm();
    } catch (error) {
      console.error('Error saving note:', error);
    }
  };

  const togglePin = async (note: any) => {
    if (!user) return;
    await updateDoc(doc(db, 'users', user.uid, 'notes', note.id), {
      isPinned: !note.isPinned,
      isArchived: false // unarchive if pinned
    });
  };

  const toggleArchive = async (note: any) => {
    if (!user) return;
    await updateDoc(doc(db, 'users', user.uid, 'notes', note.id), {
      isArchived: !note.isArchived,
      isPinned: false
    });
  };

  const deleteNote = async (id: string, e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    if (!user || !window.confirm(t('confirmDeleteNote') || 'Are you sure to delete this note?')) return;
    await deleteDoc(doc(db, 'users', user.uid, 'notes', id));
  };

  const toggleNoteChecklistItem = async (note: any, itemId: string) => {
    if (!user) return;
    const newChecklist = note.checklist.map((item: any) =>
      item.id === itemId ? { ...item, checked: !item.checked } : item
    );
    await updateDoc(doc(db, 'users', user.uid, 'notes', note.id), {
      checklist: newChecklist,
      updatedAt: new Date().toISOString()
    });
  };

  const addLabel = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && newLabel.trim() && !labels.includes(newLabel.trim())) {
      e.preventDefault();
      setLabels([...labels, newLabel.trim()]);
      setNewLabel('');
    }
  };

  const removeLabel = (labelToRemove: string) => {
    setLabels(labels.filter(l => l !== labelToRemove));
  };

  // Searching and Filtering
  let displayNotes = notes.filter(n => filter === 'all' ? !n.isArchived : n.isArchived);

  if (searchQuery) {
    const q = searchQuery.toLowerCase();
    displayNotes = displayNotes.filter(n => 
      (n.title && n.title.toLowerCase().includes(q)) || 
      (n.content && n.content.toLowerCase().includes(q)) ||
      (n.labels && n.labels.some((l: string) => l.toLowerCase().includes(q))) ||
      (n.checklist && n.checklist.some((c: any) => c.text.toLowerCase().includes(q)))
    );
  }

  const pinnedNotes = displayNotes.filter(n => n.isPinned);
  const otherNotes = displayNotes.filter(n => !n.isPinned);

  const NoteGrid = ({ notesList }: { notesList: any[] }) => (
    <div className={viewMode === 'grid' ? "columns-1 md:columns-2 lg:columns-3 gap-4 space-y-4" : "flex flex-col space-y-4"}>
      {notesList.map((note) => (
        <div key={note.id} className="break-inside-avoid">
          <NoteCard
            note={note}
            onEdit={startEditing}
            onDelete={deleteNote}
            onPin={togglePin}
            onArchive={toggleArchive}
            onToggleCheck={toggleNoteChecklistItem}
          />
        </div>
      ))}
    </div>
  );

  return (
    <div className="space-y-6 pb-20 max-w-5xl mx-auto px-4">
      {/* Header & Controls */}
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
              onChange={(e) => setSearchQuery(e.target.value)}
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

      {/* Keep-style Note Creator */}
      <div className="relative flex justify-center z-20">
        <div 
          ref={containerRef}
          className={`w-full md:w-[600px] bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-2xl shadow-lg transition-all duration-200 overflow-hidden ${isAdding ? selectedColor.bg + ' ' + selectedColor.border : ''}`}
        >
          {!isAdding ? (
            <div 
              onClick={() => setIsAdding(true)} 
              className="px-4 py-3 flex items-center justify-between cursor-pointer text-gray-500 dark:text-gray-400 font-medium"
            >
              <span>{t('takeANote') || 'Take a note...'}</span>
              <div className="flex gap-4">
                <CheckSquare size={20} onClick={(e) => { e.stopPropagation(); setIsAdding(true); setNoteType('checklist'); }} className="hover:text-gray-800 dark:hover:text-white" />
                <Palette size={20} onClick={(e) => { e.stopPropagation(); setIsAdding(true); setShowColorPicker(true); }} className="hover:text-gray-800 dark:hover:text-white" />
              </div>
            </div>
          ) : (
            <motion.div initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }} className="p-4 flex flex-col gap-3">
              <div className="flex justify-between items-start">
                <input
                  type="text"
                  placeholder={t('noteTitle') || 'Title'}
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  className="w-full bg-transparent text-lg font-bold placeholder-gray-500 dark:placeholder-gray-400 focus:outline-none text-gray-900 dark:text-white"
                />
                <button onClick={() => setIsPinned(!isPinned)} className={`p-2 rounded-full cursor-pointer transition-colors ${isPinned ? 'text-indigo-600 hover:bg-black/5 dark:hover:bg-white/5' : 'text-gray-400 hover:text-gray-800 hover:bg-black/5 dark:hover:bg-white/5'}`}>
                  <Pin size={20} fill={isPinned ? 'currentColor' : 'none'} />
                </button>
              </div>

              {noteType === 'text' ? (
                <div className="relative">
                  <textarea
                    placeholder={t('notePlaceholder') || 'Take a note...'}
                    value={content}
                    onChange={(e) => setContent(e.target.value)}
                    rows={Math.max(2, content.split('\n').length)}
                    className="w-full bg-transparent text-sm placeholder-gray-500 dark:placeholder-gray-400 focus:outline-none resize-none text-gray-900 dark:text-white leading-relaxed"
                  />
                  <div className="flex justify-end">
                    <button onClick={toggleRecording} className={`p-2 rounded-full ${isRecording ? 'text-red-500 animate-pulse bg-red-50 dark:bg-red-900/20' : 'text-gray-400 hover:text-gray-800 hover:bg-black/5 dark:hover:bg-white/5'}`}>
                      {isRecording ? <MicOff size={18} /> : <Mic size={18} />}
                    </button>
                  </div>
                </div>
              ) : (
                <div className="space-y-2 relative">
                  {checklistItems.map((item) => (
                    <div key={item.id} className="flex items-center space-x-3 group">
                      <button onClick={() => setChecklistItems(checklistItems.map(i => i.id === item.id ? { ...i, checked: !i.checked } : i))} className={`text-gray-400 ${item.checked ? 'text-indigo-500' : ''}`}>
                        {item.checked ? <CheckSquare size={18} /> : <div className="w-[18px] h-[18px] border-2 border-gray-400 rounded-sm" />}
                      </button>
                      <input 
                        type="text" value={item.text} autoFocus={!item.text}
                        onChange={(e) => setChecklistItems(checklistItems.map(i => i.id === item.id ? { ...i, text: e.target.value } : i))}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter') {
                            e.preventDefault();
                            setChecklistItems([...checklistItems, { id: Date.now().toString(), text: '', checked: false }]);
                          }
                        }}
                        className={`flex-1 bg-transparent focus:outline-none text-sm ${item.checked ? 'line-through text-gray-500' : 'text-gray-900 dark:text-white'}`}
                      />
                      <button onClick={() => setChecklistItems(checklistItems.filter(i => i.id !== item.id))} className="text-gray-400 hover:text-red-500 opacity-100 md:opacity-0 md:group-hover:opacity-100 p-1"><X size={16}/></button>
                    </div>
                  ))}
                  <div className="flex items-center space-x-3 text-gray-500 pt-1">
                    <Plus size={18} />
                    <input 
                      type="text" 
                      placeholder={t('addItem') || 'List item'} 
                      value={newItemText} 
                      onChange={(e) => setNewItemText(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' && newItemText.trim()) {
                          e.preventDefault();
                          setChecklistItems([...checklistItems, { id: Date.now().toString(), text: newItemText, checked: false }]);
                          setNewItemText('');
                        }
                      }}
                      onBlur={() => {
                        if (newItemText.trim()) {
                          setChecklistItems([...checklistItems, { id: Date.now().toString(), text: newItemText, checked: false }]);
                          setNewItemText('');
                        }
                      }}
                      className="flex-1 bg-transparent focus:outline-none text-sm placeholder-gray-500" 
                    />
                  </div>
                </div>
              )}

              {/* Labels visual display */}
              {labels.length > 0 && (
                <div className="flex flex-wrap gap-2 mt-2">
                  {labels.map(l => (
                    <span key={l} className="flex items-center gap-1 px-2 py-1 bg-black/5 dark:bg-white/10 rounded-full text-xs text-gray-700 dark:text-gray-300">
                      {l} <X size={12} className="cursor-pointer hover:text-red-500" onClick={() => removeLabel(l)} />
                    </span>
                  ))}
                </div>
              )}

              {/* Bottom Toolbar */}
              <div className="flex justify-between items-center mt-2 relative">
                <div className="flex space-x-1">
                  <div className="relative">
                    <button onClick={() => setShowColorPicker(!showColorPicker)} className="p-2 rounded-full text-gray-500 hover:bg-black/5 dark:hover:bg-white/10 transition">
                      <Palette size={18} />
                    </button>
                    {showColorPicker && (
                      <div className="absolute top-10 left-0 bg-white dark:bg-gray-800 p-2 rounded-xl shadow-xl flex gap-2 border border-gray-200 dark:border-gray-700 z-50">
                        {COLORS.map(c => (
                          <div key={c.name} onClick={() => { setSelectedColor(c); setShowColorPicker(false); }} className={`w-7 h-7 rounded-full cursor-pointer border ${c.bg} ${c.border} ${selectedColor.name === c.name ? 'ring-2 ring-indigo-500 ring-offset-2' : ''}`} />
                        ))}
                      </div>
                    )}
                  </div>
                  <button onClick={() => setNoteType(noteType === 'text' ? 'checklist' : 'text')} className="p-2 rounded-full text-gray-500 hover:bg-black/5 dark:hover:bg-white/10 transition">
                    <CheckSquare size={18} />
                  </button>
                  <div className="relative flex items-center group">
                    <button className="p-2 rounded-full text-gray-500 hover:bg-black/5 dark:hover:bg-white/10 transition">
                      <Tag size={18} />
                    </button>
                    <input 
                      type="text" 
                      placeholder="Add label..." 
                      value={newLabel}
                      onChange={(e) => setNewLabel(e.target.value)}
                      onKeyDown={addLabel}
                      className="absolute left-10 w-24 bg-transparent border-b border-gray-300 dark:border-gray-600 focus:outline-none text-xs text-gray-800 dark:text-white opacity-0 group-hover:opacity-100 focus:opacity-100 transition-opacity"
                    />
                  </div>
                </div>
                <button onClick={handleSave} className="px-5 py-1.5 font-bold text-sm bg-black dark:bg-white text-white dark:text-black rounded-lg hover:opacity-80 transition-opacity">{editingNote ? (t('update') || 'Update') : (t('close') || 'Close')}</button>
              </div>
            </motion.div>
          )}
        </div>
      </div>

      {/* Notes Grid/List Display */}
      <div className="space-y-8 mt-8">
        {pinnedNotes.length > 0 && (
          <div>
            <h3 className="text-xs font-bold text-gray-500 uppercase tracking-widest mb-4 flex items-center mb-6">
              <Pin size={14} className="mr-2" /> {t('pinned') || 'Pinned'}
            </h3>
            <NoteGrid notesList={pinnedNotes} />
          </div>
        )}

        <div>
          {pinnedNotes.length > 0 && otherNotes.length > 0 && (
            <h3 className="text-xs font-bold text-gray-500 uppercase tracking-widest mb-4 mt-8 mb-6">
              {t('others') || 'Others'}
            </h3>
          )}
          <NoteGrid notesList={otherNotes} />
        </div>

        {displayNotes.length === 0 && !isAdding && (
          <div className="text-center py-20 flex flex-col items-center opacity-50">
            <StickyNote size={64} className="mb-4 text-gray-400" />
            <h3 className="text-xl font-bold text-gray-600 dark:text-gray-300">{searchQuery ? 'No matching notes' : (t('noNotesFound') || 'Notes you add appear here')}</h3>
          </div>
        )}
      </div>
    </div>
  );
};

const NoteCard = ({ note, onEdit, onDelete, onPin, onArchive, onToggleCheck }: any) => {
  const { t } = useSettings();
  const color = COLORS.find(c => c.bg === note.color) || COLORS[0];

  return (
    <motion.div
      layout
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      exit={{ opacity: 0, scale: 0.9 }}
      onClick={() => onEdit(note)}
      className={`${color.bg} ${color.border} p-5 rounded-2xl border shadow-sm hover:shadow-md hover:-translate-y-1 transition-all group relative cursor-pointer flex flex-col h-full`}
    >
      <div className="absolute top-3 right-3 opacity-100 md:opacity-0 md:group-hover:opacity-100 transition-opacity z-10">
        <button onClick={(e) => { e.stopPropagation(); onPin(note); }} className={`p-1.5 rounded-full bg-white/50 dark:bg-gray-800/50 hover:bg-black/10 dark:hover:bg-white/20 backdrop-blur-sm ${note.isPinned ? 'text-indigo-600' : 'text-gray-600 dark:text-gray-300'}`}>
          <Pin size={16} fill={note.isPinned ? 'currentColor' : 'none'} />
        </button>
      </div>

      {note.title && <h3 className="font-bold text-lg text-gray-900 dark:text-white leading-tight mb-3 pr-8 w-full break-words">{note.title}</h3>}

      <div className="flex-1 w-full overflow-hidden">
        {note.type === 'checklist' ? (
          <div className="space-y-2 mb-4 w-full">
            {note.checklist?.slice(0, 6).map((item: any) => (
              <div key={item.id} className="flex items-start space-x-2 w-full">
                <button onClick={(e) => { e.stopPropagation(); onToggleCheck(note, item.id); }} className={`mt-0.5 flex-shrink-0 ${item.checked ? 'text-indigo-600 dark:text-indigo-400' : 'text-gray-400'}`}>
                  {item.checked ? <CheckSquare size={16} /> : <div className="w-4 h-4 border border-gray-400 rounded-sm" />}
                </button>
                <span className={`text-sm break-words flex-1 leading-snug ${item.checked ? 'line-through text-gray-500' : 'text-gray-800 dark:text-gray-200'}`}>
                  {item.text}
                </span>
              </div>
            ))}
            {note.checklist?.length > 6 && (
              <p className="text-[11px] text-gray-500 font-bold uppercase tracking-wider pl-6 pt-1">
                + {note.checklist.length - 6} {t('moreItems') || 'more items'}
              </p>
            )}
          </div>
        ) : (
          note.content && <p className="text-gray-700 dark:text-gray-300 text-sm whitespace-pre-wrap line-clamp-12 mb-4 leading-relaxed break-words">{note.content}</p>
        )}
      </div>

      {/* Labels section */}
      {(note.category) && (
        <div className="flex flex-wrap gap-1.5 mb-3 mt-auto">
          {note.category.split(',').map((label: string) => (
            <span key={label} className="px-2 py-0.5 bg-black/5 dark:bg-white/10 text-gray-600 dark:text-gray-300 text-[10px] rounded-full font-bold truncate max-w-full">
              {label.trim()}
            </span>
          ))}
        </div>
      )}

      {/* Footer controls */}
      <div className="flex justify-between items-center pt-3 border-t border-black/5 dark:border-white/10 mt-auto opacity-100 md:opacity-0 md:group-hover:opacity-100 transition-opacity">
        <div className="flex space-x-1">
          <button onClick={(e) => { e.stopPropagation(); onArchive(note); }} className={`p-1.5 rounded-full hover:bg-black/5 dark:hover:bg-white/10 ${note.isArchived ? 'text-indigo-600' : 'text-gray-500'}`} title="Archive">
            <Archive size={16} />
          </button>
        </div>
        <button onClick={(e) => deleteNote(note.id, e)} className="p-1.5 rounded-full hover:bg-red-100 hover:text-red-600 dark:hover:bg-red-900/30 text-gray-500 transition-colors" title="Delete">
          <Trash2 size={16} />
        </button>
      </div>
    </motion.div>
  );
};
