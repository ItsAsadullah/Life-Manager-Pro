import React, { useEffect, useState, useRef } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useSettings } from '../contexts/SettingsContext';
import { useLocation } from 'react-router-dom';
import { collection, query, onSnapshot, orderBy, addDoc, updateDoc, deleteDoc, doc, serverTimestamp } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { Mic, MicOff, Plus, Save, X, CheckSquare, Type, Palette, Pin, Trash2, Archive, MoreVertical, Check, GripVertical, StickyNote } from 'lucide-react';
import { format } from 'date-fns';
import { motion, AnimatePresence } from 'motion/react';

const COLORS = [
  { name: 'Default', bg: 'bg-white', border: 'border-gray-200' },
  { name: 'Red', bg: 'bg-red-50', border: 'border-red-200' },
  { name: 'Orange', bg: 'bg-orange-50', border: 'border-orange-200' },
  { name: 'Yellow', bg: 'bg-yellow-50', border: 'border-yellow-200' },
  { name: 'Green', bg: 'bg-green-50', border: 'border-green-200' },
  { name: 'Teal', bg: 'bg-teal-50', border: 'border-teal-200' },
  { name: 'Blue', bg: 'bg-blue-50', border: 'border-blue-200' },
  { name: 'Purple', bg: 'bg-purple-50', border: 'border-purple-200' },
  { name: 'Pink', bg: 'bg-pink-50', border: 'border-pink-200' },
];

export const Notes: React.FC = () => {
  const { user } = useAuth();
  const { t } = useSettings();
  const location = useLocation();
  const [notes, setNotes] = useState<any[]>([]);
  const [isAdding, setIsAdding] = useState(false);
  const [editingNote, setEditingNote] = useState<any>(null);

  // Form states
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [category, setCategory] = useState('Personal');
  const [noteType, setNoteType] = useState<'text' | 'checklist'>('text');
  const [checklistItems, setChecklistItems] = useState<{ id: string; text: string; checked: boolean }[]>([]);
  const [selectedColor, setSelectedColor] = useState(COLORS[0]);
  const [isPinned, setIsPinned] = useState(false);
  const [isRecording, setIsRecording] = useState(false);
  const [showColorPicker, setShowColorPicker] = useState(false);
  const [filter, setFilter] = useState<'all' | 'archived'>('all');

  useEffect(() => {
    if (location.state?.openAddModal) {
      setIsAdding(true);
      window.history.replaceState({}, document.title);
    }
  }, [location]);

  const resetForm = () => {
    setTitle('');
    setContent('');
    setCategory('Personal');
    setNoteType('text');
    setChecklistItems([]);
    setSelectedColor(COLORS[0]);
    setIsPinned(false);
    setEditingNote(null);
    setIsAdding(false);
  };

  const startEditing = (note: any) => {
    setEditingNote(note);
    setTitle(note.title);
    setContent(note.content || '');
    setCategory(note.category || 'Personal');
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

    // Setup Speech Recognition
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
    if (!user || !title.trim()) return;
    if (noteType === 'text' && !content.trim()) return;
    if (noteType === 'checklist' && checklistItems.length === 0) return;
    
    try {
      const noteData = {
        title,
        content: noteType === 'text' ? content : '',
        type: noteType,
        checklist: noteType === 'checklist' ? checklistItems : [],
        category,
        color: selectedColor.bg,
        isPinned,
        isArchived: false,
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
      isPinned: !note.isPinned
    });
  };

  const toggleArchive = async (note: any) => {
    if (!user) return;
    await updateDoc(doc(db, 'users', user.uid, 'notes', note.id), {
      isArchived: !note.isArchived,
      isPinned: false
    });
  };

  const deleteNote = async (id: string) => {
    if (!user || !window.confirm('Are you sure you want to delete this note?')) return;
    await deleteDoc(doc(db, 'users', user.uid, 'notes', id));
  };

  const addChecklistItem = () => {
    setChecklistItems([...checklistItems, { id: Date.now().toString(), text: '', checked: false }]);
  };

  const updateChecklistItem = (id: string, text: string) => {
    setChecklistItems(checklistItems.map(item => item.id === id ? { ...item, text } : item));
  };

  const toggleChecklistItem = (id: string) => {
    setChecklistItems(checklistItems.map(item => item.id === id ? { ...item, checked: !item.checked } : item));
  };

  const removeChecklistItem = (id: string) => {
    setChecklistItems(checklistItems.filter(item => item.id !== id));
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

  const filteredNotes = notes.filter(n => filter === 'all' ? !n.isArchived : n.isArchived);
  const pinnedNotes = filteredNotes.filter(n => n.isPinned);
  const otherNotes = filteredNotes.filter(n => !n.isPinned);

  return (
    <div className="space-y-6 pb-20">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">{t('notes')}</h2>
          <div className="flex space-x-4 mt-1">
            <button 
              onClick={() => setFilter('all')}
              className={`text-sm font-medium ${filter === 'all' ? 'text-indigo-600 underline' : 'text-gray-500'}`}
            >
              {t('allNotes')}
            </button>
            <button 
              onClick={() => setFilter('archived')}
              className={`text-sm font-medium ${filter === 'archived' ? 'text-indigo-600 underline' : 'text-gray-500'}`}
            >
              {t('archived')}
            </button>
          </div>
        </div>
        <button
          onClick={() => setIsAdding(true)}
          className="flex items-center px-4 py-2 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 shadow-lg shadow-indigo-100 transition-all"
        >
          <Plus size={20} className="mr-2" />
          {t('addNote')}
        </button>
      </div>

      <AnimatePresence>
        {isAdding && (
          <motion.div 
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.95 }}
            className={`p-6 rounded-2xl shadow-xl border ${selectedColor.bg} ${selectedColor.border} mb-6 relative overflow-hidden`}
          >
            <div className="flex justify-between items-center mb-4">
              <div className="flex space-x-2">
                <button 
                  onClick={() => setNoteType('text')}
                  className={`p-2 rounded-lg transition-colors ${noteType === 'text' ? 'bg-black/10' : 'hover:bg-black/5'}`}
                  title="Text Note"
                >
                  <Type size={18} />
                </button>
                <button 
                  onClick={() => setNoteType('checklist')}
                  className={`p-2 rounded-lg transition-colors ${noteType === 'checklist' ? 'bg-black/10' : 'hover:bg-black/5'}`}
                  title="Checklist"
                >
                  <CheckSquare size={18} />
                </button>
                <div className="relative">
                  <button 
                    onClick={() => setShowColorPicker(!showColorPicker)}
                    className="p-2 rounded-lg hover:bg-black/5 transition-colors"
                    title="Change Color"
                  >
                    <Palette size={18} />
                  </button>
                  {showColorPicker && (
                    <div className="absolute top-full left-0 mt-2 p-2 bg-white rounded-xl shadow-2xl border border-gray-100 flex space-x-2 z-10">
                      {COLORS.map(color => (
                        <button
                          key={color.name}
                          onClick={() => {
                            setSelectedColor(color);
                            setShowColorPicker(false);
                          }}
                          className={`w-6 h-6 rounded-full border ${color.bg} ${color.border} ${selectedColor.name === color.name ? 'ring-2 ring-indigo-500 ring-offset-2' : ''}`}
                        />
                      ))}
                    </div>
                  )}
                </div>
                <button 
                  onClick={() => setIsPinned(!isPinned)}
                  className={`p-2 rounded-lg transition-colors ${isPinned ? 'bg-black/10 text-indigo-600' : 'hover:bg-black/5'}`}
                  title="Pin Note"
                >
                  <Pin size={18} fill={isPinned ? 'currentColor' : 'none'} />
                </button>
              </div>
              <button onClick={resetForm} className="text-gray-500 hover:text-gray-700">
                <X size={20} />
              </button>
            </div>
            
            <div className="space-y-4">
              <input
                type="text"
                placeholder="Note Title"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="w-full bg-transparent text-xl font-bold placeholder:text-gray-400 focus:outline-none"
              />
              
              <div className="flex items-center space-x-2">
                <span className="text-xs font-bold text-gray-400 uppercase tracking-widest">Category:</span>
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="bg-transparent text-xs font-bold text-indigo-600 focus:outline-none cursor-pointer"
                >
                  <option value="Personal">Personal</option>
                  <option value="Work">Work</option>
                  <option value="Idea">Idea</option>
                  <option value="Reminder">Reminder</option>
                </select>
              </div>

              {noteType === 'text' ? (
                <div className="relative">
                  <textarea
                    placeholder="Write your note here or use voice typing..."
                    value={content}
                    onChange={(e) => setContent(e.target.value)}
                    rows={5}
                    className="w-full bg-transparent placeholder:text-gray-400 focus:outline-none resize-none"
                  />
                  <button
                    onClick={toggleRecording}
                    className={`absolute bottom-0 right-0 p-3 rounded-full shadow-lg ${
                      isRecording ? 'bg-red-500 text-white animate-pulse' : 'bg-white text-gray-600 hover:bg-gray-50'
                    }`}
                    title={isRecording ? 'Stop recording' : 'Start voice typing'}
                  >
                    {isRecording ? <MicOff size={20} /> : <Mic size={20} />}
                  </button>
                </div>
              ) : (
                <div className="space-y-2">
                  {checklistItems.map((item) => (
                    <div key={item.id} className="flex items-center space-x-2 group">
                      <button 
                        onClick={() => toggleChecklistItem(item.id)}
                        className={`p-1 rounded transition-colors ${item.checked ? 'text-indigo-600' : 'text-gray-400'}`}
                      >
                        {item.checked ? <CheckSquare size={18} /> : <div className="w-[18px] h-[18px] border-2 border-gray-300 rounded" />}
                      </button>
                      <input
                        type="text"
                        value={item.text}
                        onChange={(e) => updateChecklistItem(item.id, e.target.value)}
                        placeholder="List item..."
                        className={`flex-1 bg-transparent focus:outline-none ${item.checked ? 'line-through text-gray-400' : ''}`}
                        autoFocus={!item.text}
                      />
                      <button 
                        onClick={() => removeChecklistItem(item.id)}
                        className="p-1 text-gray-400 opacity-0 group-hover:opacity-100 hover:text-red-500 transition-all"
                      >
                        <X size={14} />
                      </button>
                    </div>
                  ))}
                  <button 
                    onClick={addChecklistItem}
                    className="flex items-center text-sm text-gray-500 hover:text-indigo-600 transition-colors mt-2"
                  >
                    <Plus size={16} className="mr-1" /> Add item
                  </button>
                </div>
              )}

              <div className="flex justify-end pt-4">
                <button
                  onClick={handleSave}
                  disabled={!title.trim() || (noteType === 'text' ? !content.trim() : checklistItems.length === 0)}
                  className="flex items-center px-6 py-2.5 bg-black text-white rounded-xl hover:bg-gray-800 disabled:opacity-50 shadow-lg transition-all font-bold"
                >
                  <Save size={18} className="mr-2" />
                  {editingNote ? 'Update Note' : 'Save Note'}
                </button>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <div className="space-y-8">
        {pinnedNotes.length > 0 && (
          <div>
            <h3 className="text-xs font-bold text-gray-400 uppercase tracking-widest mb-4 flex items-center">
              <Pin size={14} className="mr-2" /> Pinned
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {pinnedNotes.map(note => (
                <NoteCard 
                  key={note.id} 
                  note={note} 
                  onEdit={startEditing} 
                  onDelete={deleteNote} 
                  onPin={togglePin} 
                  onArchive={toggleArchive}
                  onToggleCheck={toggleNoteChecklistItem}
                />
              ))}
            </div>
          </div>
        )}

        <div>
          {pinnedNotes.length > 0 && (
            <h3 className="text-xs font-bold text-gray-400 uppercase tracking-widest mb-4">Others</h3>
          )}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {otherNotes.map(note => (
              <NoteCard 
                key={note.id} 
                note={note} 
                onEdit={startEditing} 
                onDelete={deleteNote} 
                onPin={togglePin} 
                onArchive={toggleArchive}
                onToggleCheck={toggleNoteChecklistItem}
              />
            ))}
          </div>
        </div>

        {filteredNotes.length === 0 && !isAdding && (
          <div className="text-center py-20 bg-white rounded-3xl border border-dashed border-gray-200">
            <div className="w-16 h-16 bg-gray-50 rounded-full flex items-center justify-center mx-auto mb-4">
              <StickyNote className="text-gray-300" size={32} />
            </div>
            <h3 className="text-lg font-bold text-gray-900">No notes found</h3>
            <p className="text-gray-500">Create your first note to stay organized!</p>
          </div>
        )}
      </div>
    </div>
  );
};

const NoteCard = ({ note, onEdit, onDelete, onPin, onArchive, onToggleCheck }: any) => {
  const color = COLORS.find(c => c.bg === note.color) || COLORS[0];
  
  return (
    <motion.div 
      layout
      className={`${color.bg} ${color.border} p-6 rounded-2xl shadow-sm border hover:shadow-md transition-all group relative`}
    >
      <div className="flex justify-between items-start mb-3">
        <h3 className="font-bold text-lg text-gray-900 leading-tight">{note.title}</h3>
        <div className="flex space-x-1 opacity-0 group-hover:opacity-100 transition-opacity">
          <button 
            onClick={() => onPin(note)}
            className={`p-1.5 rounded-lg hover:bg-black/5 ${note.isPinned ? 'text-indigo-600' : 'text-gray-400'}`}
          >
            <Pin size={16} fill={note.isPinned ? 'currentColor' : 'none'} />
          </button>
          <button 
            onClick={() => onArchive(note)}
            className={`p-1.5 rounded-lg hover:bg-black/5 ${note.isArchived ? 'text-indigo-600' : 'text-gray-400'}`}
          >
            <Archive size={16} />
          </button>
          <button 
            onClick={() => onDelete(note.id)}
            className="p-1.5 rounded-lg hover:bg-red-50 text-gray-400 hover:text-red-500"
          >
            <Trash2 size={16} />
          </button>
        </div>
      </div>

      <div onClick={() => onEdit(note)} className="cursor-pointer">
        {note.type === 'checklist' ? (
          <div className="space-y-1.5 mb-4">
            {note.checklist?.slice(0, 5).map((item: any) => (
              <div key={item.id} className="flex items-center space-x-2">
                <button 
                  onClick={(e) => {
                    e.stopPropagation();
                    onToggleCheck(note, item.id);
                  }}
                  className={`flex-shrink-0 ${item.checked ? 'text-indigo-600' : 'text-gray-300'}`}
                >
                  {item.checked ? <CheckSquare size={14} /> : <div className="w-[14px] h-[14px] border border-gray-400 rounded-sm" />}
                </button>
                <span className={`text-sm text-gray-700 truncate ${item.checked ? 'line-through text-gray-400' : ''}`}>
                  {item.text}
                </span>
              </div>
            ))}
            {note.checklist?.length > 5 && (
              <p className="text-[10px] text-gray-400 font-bold uppercase tracking-wider mt-2">
                + {note.checklist.length - 5} more items
              </p>
            )}
          </div>
        ) : (
          <p className="text-gray-600 text-sm whitespace-pre-wrap line-clamp-6 mb-4">{note.content}</p>
        )}
      </div>

      <div className="flex justify-between items-center pt-4 border-t border-black/5">
        <span className="px-2 py-0.5 bg-black/5 text-gray-600 text-[10px] rounded-full font-bold uppercase tracking-wider">
          {note.category || 'Personal'}
        </span>
        <span className="text-[10px] text-gray-400 font-medium">
          {note.createdAt ? format(new Date(note.createdAt), 'MMM d, yyyy') : ''}
        </span>
      </div>
    </motion.div>
  );
};
