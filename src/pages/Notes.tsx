import React, { useEffect, useState, useRef } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { collection, query, onSnapshot, orderBy, addDoc, serverTimestamp } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { Mic, MicOff, Plus, Save, X } from 'lucide-react';
import { format } from 'date-fns';

export const Notes: React.FC = () => {
  const { user } = useAuth();
  const [notes, setNotes] = useState<any[]>([]);
  const [isAdding, setIsAdding] = useState(false);
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [category, setCategory] = useState('Personal');
  const [isRecording, setIsRecording] = useState(false);
  
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
    if (!user || !title.trim() || !content.trim()) return;
    
    try {
      await addDoc(collection(db, 'users', user.uid, 'notes'), {
        title,
        content,
        category,
        isVoiceNote: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      });
      setIsAdding(false);
      setTitle('');
      setContent('');
    } catch (error) {
      console.error('Error adding note:', error);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-bold text-gray-900">Notes</h2>
        <button
          onClick={() => setIsAdding(true)}
          className="flex items-center px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
        >
          <Plus size={20} className="mr-2" />
          Add Note
        </button>
      </div>

      {isAdding && (
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200 mb-6">
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-lg font-bold">New Note</h3>
            <button onClick={() => setIsAdding(false)} className="text-gray-400 hover:text-gray-600">
              <X size={20} />
            </button>
          </div>
          
          <div className="space-y-4">
            <input
              type="text"
              placeholder="Note Title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            />
            
            <select
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            >
              <option value="Personal">Personal</option>
              <option value="Work">Work</option>
              <option value="Idea">Idea</option>
              <option value="Reminder">Reminder</option>
            </select>

            <div className="relative">
              <textarea
                placeholder="Write your note here or use voice typing..."
                value={content}
                onChange={(e) => setContent(e.target.value)}
                rows={5}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              />
              <button
                onClick={toggleRecording}
                className={`absolute bottom-4 right-4 p-2 rounded-full ${
                  isRecording ? 'bg-red-100 text-red-600 animate-pulse' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                }`}
                title={isRecording ? 'Stop recording' : 'Start voice typing'}
              >
                {isRecording ? <MicOff size={20} /> : <Mic size={20} />}
              </button>
            </div>

            <button
              onClick={handleSave}
              disabled={!title.trim() || !content.trim()}
              className="flex items-center px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50"
            >
              <Save size={20} className="mr-2" />
              Save Note
            </button>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {notes.map(note => (
          <div key={note.id} className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 hover:shadow-md transition-shadow">
            <div className="flex justify-between items-start mb-2">
              <h3 className="font-bold text-lg text-gray-900">{note.title}</h3>
              <span className="px-2 py-1 bg-indigo-50 text-indigo-700 text-xs rounded-full font-medium">
                {note.category || 'Personal'}
              </span>
            </div>
            <p className="text-gray-600 whitespace-pre-wrap line-clamp-4">{note.content}</p>
            <div className="mt-4 pt-4 border-t border-gray-100 flex justify-between items-center text-xs text-gray-400">
              <span>{note.createdAt ? format(new Date(note.createdAt), 'MMM d, yyyy') : ''}</span>
            </div>
          </div>
        ))}
        {notes.length === 0 && !isAdding && (
          <div className="col-span-full text-center py-12 text-gray-500">
            No notes found. Create one to get started!
          </div>
        )}
      </div>
    </div>
  );
};
